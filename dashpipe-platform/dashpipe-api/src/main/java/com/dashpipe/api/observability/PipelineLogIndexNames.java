package com.dashpipe.api.observability;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Elasticsearch index naming for pipeline logs (architecture §7.3):
 *
 * <pre>
 * pipeline-logs-{tenant_id}-{YYYY.MM.DD}
 * </pre>
 */
public final class PipelineLogIndexNames {

  private static final DateTimeFormatter DAY =
      DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC);

  private PipelineLogIndexNames() {}

  public static String forTenantAndInstant(String tenantId, Instant instant) {
    String tenant = sanitizeTenant(tenantId);
    Instant when = instant == null ? Instant.now() : instant;
    return "pipeline-logs-" + tenant + "-" + DAY.format(when);
  }

  public static String kibanaPattern(String tenantId) {
    return "pipeline-logs-" + sanitizeTenant(tenantId) + "-*";
  }

  private static String sanitizeTenant(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "unknown";
    }
    // ES index names: lowercase; replace unsafe chars
    return tenantId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
  }
}
