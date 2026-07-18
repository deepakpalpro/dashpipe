package com.dashpipe.api.observability;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.dashpipe.api.config.ObservabilityPortalProperties;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantContextRequiredException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portal links to optional external tools (Grafana, Elasticsearch/Kibana). Driven by {@code
 * pipeline.observability.*} settings — blank URLs hide the corresponding link.
 */
@RestController
@RequestMapping("/api/v1/observability")
public class ObservabilityLinksController {

  private final ObservabilityPortalProperties properties;

  public ObservabilityLinksController(ObservabilityPortalProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/links")
  public ObservabilityDtos.PortalLinksResponse links(
      @RequestParam(value = "pipelineId", required = false) String pipelineId,
      @RequestParam(value = "executionId", required = false) String executionId) {
    requireTenantId();
    String grafana = properties.normalizedGrafanaUrl();
    String elasticsearch = properties.normalizedElasticsearchUrl();
    return new ObservabilityDtos.PortalLinksResponse(
        properties.isGrafanaEnabled(),
        grafana == null
            ? null
            : appendContext(grafana, pipelineId, executionId),
        blankToDefault(properties.getGrafanaLabel(), "Grafana"),
        properties.isElasticsearchEnabled(),
        elasticsearch == null
            ? null
            : appendContext(elasticsearch, pipelineId, executionId),
        blankToDefault(properties.getElasticsearchLabel(), "Elasticsearch"));
  }

  private static String appendContext(String base, String pipelineId, String executionId) {
    StringBuilder url = new StringBuilder(base);
    boolean hasQuery = base.contains("?");
    if (pipelineId != null && !pipelineId.isBlank()) {
      url.append(hasQuery ? '&' : '?').append("pipelineId=").append(encode(pipelineId));
      hasQuery = true;
    }
    if (executionId != null && !executionId.isBlank()) {
      url.append(hasQuery ? '&' : '?').append("executionId=").append(encode(executionId));
    }
    return url.toString();
  }

  private static String encode(String value) {
    return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String blankToDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new TenantContextRequiredException();
    }
    return tenantId;
  }
}
