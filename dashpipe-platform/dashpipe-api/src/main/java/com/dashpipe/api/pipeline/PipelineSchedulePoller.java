package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fires ACTIVE pipelines on a cron schedule.
 *
 * <p>Cron resolution order (UI values win over a stale column):
 *
 * <ol>
 *   <li>{@code execution_config.scheduleCron} / {@code schedule_cron} / {@code cron}
 *   <li>first {@code plet-schedule-source} step {@code execution_config.cron}
 *   <li>{@code pipelines.schedule_cron}
 * </ol>
 */
@Component
@EnableScheduling
@ConditionalOnProperty(
    prefix = "pipeline.schedule",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PipelineSchedulePoller {

  private static final Logger log = LoggerFactory.getLogger(PipelineSchedulePoller.class);
  static final String SCHEDULE_SOURCE_PIPELET = "plet-schedule-source";

  private final PipelineRepository pipelineRepository;
  private final PipelineStepRepository stepRepository;
  private final PipelineExecutionRepository executionRepository;
  private final PipelineRunOrchestrator orchestrator;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Map<String, Instant> lastFiredMinute = new ConcurrentHashMap<>();

  public PipelineSchedulePoller(
      PipelineRepository pipelineRepository,
      PipelineStepRepository stepRepository,
      PipelineExecutionRepository executionRepository,
      PipelineRunOrchestrator orchestrator,
      ObjectMapper objectMapper) {
    this.pipelineRepository = pipelineRepository;
    this.stepRepository = stepRepository;
    this.executionRepository = executionRepository;
    this.orchestrator = orchestrator;
    this.objectMapper = objectMapper;
    this.clock = Clock.systemUTC();
  }

  @Scheduled(fixedDelayString = "${pipeline.schedule.poll-interval-ms:15000}")
  @Transactional
  public void poll() {
    Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MINUTES);
    List<Pipeline> active = pipelineRepository.findByStatus(PipelineStatus.ACTIVE);
    for (Pipeline pipeline : active) {
      try {
        maybeFire(pipeline, now);
      } catch (Exception ex) {
        log.warn(
            "schedule poll failed pipelineId={} cron={}: {}",
            pipeline.getId(),
            pipeline.getScheduleCron(),
            ex.toString());
      }
    }
  }

  private void maybeFire(Pipeline pipeline, Instant nowMinute) {
    List<PipelineStep> steps = stepRepository.findByPipelineIdOrdered(pipeline.getId());
    String cron = resolveCron(pipeline, steps);
    if (cron == null || cron.isBlank()) {
      return;
    }
    // Keep column in sync when UI only updated execution_config / step cron.
    if (!cron.equals(pipeline.getScheduleCron())) {
      pipeline.setScheduleCron(cron);
      mergeScheduleCronIntoExecutionConfig(pipeline, cron);
      pipelineRepository.save(pipeline);
    }
    if (!matchesCurrentMinute(cron, nowMinute)) {
      return;
    }
    Instant previous = lastFiredMinute.put(pipeline.getId(), nowMinute);
    if (previous != null && previous.equals(nowMinute)) {
      return;
    }
    boolean busy =
        executionRepository.findByStatus(ExecutionStatus.RUNNING).stream()
                .anyMatch(e -> pipeline.getId().equals(e.getPipelineId()))
            || executionRepository.findByStatus(ExecutionStatus.PENDING).stream()
                .anyMatch(e -> pipeline.getId().equals(e.getPipelineId()));
    if (busy) {
      log.debug("skip schedule fire; pipeline already running pipelineId={}", pipeline.getId());
      return;
    }
    if (steps.isEmpty()) {
      log.debug("skip schedule fire; no steps pipelineId={}", pipeline.getId());
      return;
    }
    PipelineExecution execution =
        orchestrator.start(pipeline, steps, ExecutionTrigger.SCHEDULE);
    log.info(
        "scheduled pipeline run pipelineId={} executionId={} cron={}",
        pipeline.getId(),
        execution.getId(),
        cron);
  }

  static String resolveCron(Pipeline pipeline, List<PipelineStep> steps) {
    String fromPipelineConfig = cronFromJson(pipeline.getExecutionConfig());
    if (fromPipelineConfig != null) {
      return fromPipelineConfig;
    }
    if (steps != null) {
      for (PipelineStep step : steps) {
        if (!SCHEDULE_SOURCE_PIPELET.equals(step.getPipeletId())) {
          continue;
        }
        String raw =
            step.getExecutionConfig() != null && !step.getExecutionConfig().isBlank()
                ? step.getExecutionConfig()
                : step.getConfig();
        String fromStep = cronFromJson(raw);
        if (fromStep != null) {
          return fromStep;
        }
      }
    }
    if (pipeline.getScheduleCron() != null && !pipeline.getScheduleCron().isBlank()) {
      return pipeline.getScheduleCron().trim();
    }
    return null;
  }

  static String cronFromJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return cronFromNode(new ObjectMapper().readTree(json));
    } catch (Exception ex) {
      return null;
    }
  }

  static String cronFromNode(JsonNode executionConfig) {
    if (executionConfig == null || executionConfig.isNull() || !executionConfig.isObject()) {
      return null;
    }
    for (String key : List.of("scheduleCron", "schedule_cron", "cron")) {
      JsonNode node = executionConfig.get(key);
      if (node != null && node.isTextual() && !node.asText().isBlank()) {
        return node.asText().trim();
      }
    }
    return null;
  }

  private void mergeScheduleCronIntoExecutionConfig(Pipeline pipeline, String cron) {
    try {
      JsonNode existing =
          pipeline.getExecutionConfig() == null || pipeline.getExecutionConfig().isBlank()
              ? objectMapper.createObjectNode()
              : objectMapper.readTree(pipeline.getExecutionConfig());
      ObjectNode out =
          existing != null && existing.isObject()
              ? (ObjectNode) existing.deepCopy()
              : objectMapper.createObjectNode();
      out.put("scheduleCron", cron);
      pipeline.setExecutionConfig(objectMapper.writeValueAsString(out));
    } catch (Exception ex) {
      log.debug("could not merge scheduleCron into execution_config: {}", ex.toString());
    }
  }

  static boolean matchesCurrentMinute(String cron, Instant nowMinute) {
    CronExpression expression = parseCron(cron);
    if (expression == null) {
      return false;
    }
    ZonedDateTime zoned = nowMinute.atZone(ZoneOffset.UTC);
    ZonedDateTime previous = zoned.minusMinutes(1);
    ZonedDateTime next = expression.next(previous);
    return next != null
        && next.toInstant().truncatedTo(ChronoUnit.MINUTES).equals(nowMinute);
  }

  static CronExpression parseCron(String cron) {
    String trimmed = cron.trim();
    try {
      return CronExpression.parse(trimmed);
    } catch (IllegalArgumentException first) {
      String[] parts = trimmed.split("\\s+");
      if (parts.length == 5) {
        try {
          return CronExpression.parse("0 " + trimmed);
        } catch (IllegalArgumentException ignored) {
          return null;
        }
      }
      return null;
    }
  }
}
