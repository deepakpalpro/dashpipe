package com.dashflow.api.observability;

import com.dashflow.api.pipeline.Pipeline;
import com.dashflow.api.pipeline.PipelineExecution;
import com.dashflow.api.pipeline.PipelineExecutionNotFoundException;
import com.dashflow.api.pipeline.PipelineExecutionRepository;
import com.dashflow.api.pipeline.PipelineNotFoundException;
import com.dashflow.api.pipeline.PipelineRepository;
import com.dashflow.api.tenant.TenantContext;
import com.dashflow.api.tenant.TenantContextRequiredException;
import com.dashflow.api.tenant.TenantFilters;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObservabilityService {

  /** Architecture §7.5 Grafana rule: stale when older than 90s. */
  public static final long STALE_AFTER_SECONDS = 90L;

  private final PipelineRepository pipelineRepository;
  private final PipelineExecutionRepository executionRepository;
  private final PipelineLogIndexer logIndexer;
  private final MeterRegistry meterRegistry;
  private final EntityManager entityManager;

  public ObservabilityService(
      PipelineRepository pipelineRepository,
      PipelineExecutionRepository executionRepository,
      PipelineLogIndexer logIndexer,
      MeterRegistry meterRegistry,
      EntityManager entityManager) {
    this.pipelineRepository = pipelineRepository;
    this.executionRepository = executionRepository;
    this.logIndexer = logIndexer;
    this.meterRegistry = meterRegistry;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public ObservabilityDtos.CompletenessResponse completeness(String pipelineId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    Pipeline pipeline = requirePipeline(pipelineId);

    PipelineExecution latest =
        executionRepository.findFilteredByPipelineId(pipelineId).stream()
            .findFirst()
            .orElse(null);

    if (latest == null) {
      return new ObservabilityDtos.CompletenessResponse(
          pipeline.getId(),
          tenantId,
          null,
          0L,
          0L,
          BigDecimal.ZERO.setScale(2),
          0.0);
    }

    CompletenessCalculator.Result calc =
        CompletenessCalculator.calculate(latest.getRecordsIn(), latest.getRecordsOut());
    BigDecimal pct =
        latest.getCompletenessPct() != null ? latest.getCompletenessPct() : calc.percent();
    return new ObservabilityDtos.CompletenessResponse(
        pipeline.getId(),
        tenantId,
        latest.getId(),
        latest.getRecordsIn(),
        latest.getRecordsOut(),
        pct,
        calc.ratio());
  }

  @Transactional(readOnly = true)
  public ObservabilityDtos.LatencyResponse latency(String pipelineId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    requirePipeline(pipelineId);

    List<Timer> timers =
        meterRegistry.find(PipeletMetricsEmitter.PROCESSING_DURATION)
            .tag(PipeletMetricsEmitter.TAG_TENANT, tenantId)
            .tag(PipeletMetricsEmitter.TAG_PIPELINE, pipelineId)
            .timers()
            .stream()
            .toList();

    long samples = timers.stream().mapToLong(Timer::count).sum();
    double totalNanos =
        timers.stream().mapToDouble(t -> t.totalTime(TimeUnit.NANOSECONDS)).sum();
    double maxNanos =
        timers.stream().mapToDouble(t -> t.max(TimeUnit.NANOSECONDS)).max().orElse(0.0);
    double meanMs = samples == 0 ? 0.0 : (totalNanos / samples) / 1_000_000.0;
    double maxMs = maxNanos / 1_000_000.0;

    return new ObservabilityDtos.LatencyResponse(pipelineId, tenantId, samples, meanMs, maxMs);
  }

  @Transactional(readOnly = true)
  public ObservabilityDtos.HeartbeatResponse heartbeat(String pipelineId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    requirePipeline(pipelineId);

    Long latest =
        meterRegistry.find(PipeletMetricsEmitter.HEARTBEAT_TIMESTAMP)
            .tag(PipeletMetricsEmitter.TAG_TENANT, tenantId)
            .tag(PipeletMetricsEmitter.TAG_PIPELINE, pipelineId)
            .gauges()
            .stream()
            .mapToLong(g -> (long) g.value())
            .max()
            .stream()
            .boxed()
            .findFirst()
            .orElse(null);

    boolean stale =
        latest == null || Instant.now().getEpochSecond() - latest > STALE_AFTER_SECONDS;
    return new ObservabilityDtos.HeartbeatResponse(pipelineId, tenantId, latest, stale);
  }

  @Transactional(readOnly = true)
  public ObservabilityDtos.ErrorSummaryResponse errors(String pipelineId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    requirePipeline(pipelineId);

    List<ObservabilityDtos.ErrorTypeCount> byType = new ArrayList<>();
    double total = 0.0;
    for (Counter counter :
        meterRegistry
            .find(PipeletMetricsEmitter.ERRORS_TOTAL)
            .tag(PipeletMetricsEmitter.TAG_TENANT, tenantId)
            .tag(PipeletMetricsEmitter.TAG_PIPELINE, pipelineId)
            .counters()) {
      String type = counter.getId().getTag(PipeletMetricsEmitter.TAG_ERROR_TYPE);
      if (type == null) {
        type = "unknown";
      }
      double count = counter.count();
      total += count;
      byType.add(new ObservabilityDtos.ErrorTypeCount(type, count));
    }
    byType.sort(Comparator.comparing(ObservabilityDtos.ErrorTypeCount::errorType));
    return new ObservabilityDtos.ErrorSummaryResponse(pipelineId, tenantId, total, byType);
  }

  @Transactional(readOnly = true)
  public ObservabilityDtos.ExecutionLogsResponse logs(String executionId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    PipelineExecution execution =
        executionRepository
            .findFilteredById(executionId)
            .orElseThrow(() -> new PipelineExecutionNotFoundException(executionId));

    List<ObservabilityDtos.LogEntry> entries =
        logIndexer.findByExecutionId(executionId).stream()
            .map(
                d ->
                    new ObservabilityDtos.LogEntry(
                        d.timestamp(),
                        d.level(),
                        d.pipeletId(),
                        d.podName(),
                        d.message(),
                        d.recordsIn(),
                        d.recordsOut(),
                        d.durationMs()))
            .toList();

    return new ObservabilityDtos.ExecutionLogsResponse(
        executionId, tenantId, execution.getPipelineId(), entries);
  }

  private Pipeline requirePipeline(String pipelineId) {
    return pipelineRepository
        .findFilteredById(pipelineId)
        .orElseThrow(() -> new PipelineNotFoundException(pipelineId));
  }

  private static String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new TenantContextRequiredException();
    }
    return tenantId;
  }

  private void enableTenantFilter(String tenantId) {
    Session session = entityManager.unwrap(Session.class);
    var filter = session.getEnabledFilter(TenantFilters.NAME);
    if (filter == null) {
      filter = session.enableFilter(TenantFilters.NAME);
    }
    filter.setParameter(TenantFilters.PARAM_TENANT_ID, tenantId);
  }
}
