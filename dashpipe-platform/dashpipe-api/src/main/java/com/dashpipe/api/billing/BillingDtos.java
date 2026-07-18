package com.dashpipe.api.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** §3.5 usage / billing / quota response DTOs. */
public final class BillingDtos {

  private BillingDtos() {}

  public record DimensionUsage(
      @JsonProperty("quantity") BigDecimal quantity, @JsonProperty("cost") BigDecimal cost) {}

  public record UsageSummaryResponse(
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("period_start") Instant periodStart,
      @JsonProperty("period_end") Instant periodEnd,
      @JsonProperty("dimensions") Map<String, DimensionUsage> dimensions,
      @JsonProperty("total_cost") BigDecimal totalCost,
      @JsonProperty("credit_balance") BigDecimal creditBalance) {}

  public record UsageEventItem(
      @JsonProperty("id") String id,
      @JsonProperty("dimension") String dimension,
      @JsonProperty("quantity") BigDecimal quantity,
      @JsonProperty("unit") String unit,
      @JsonProperty("execution_id") String executionId,
      @JsonProperty("pipeline_id") String pipelineId,
      @JsonProperty("pipelet_id") String pipeletId,
      @JsonProperty("connector_id") String connectorId,
      @JsonProperty("recorded_at") Instant recordedAt,
      @JsonProperty("idempotency_key") String idempotencyKey) {}

  public record UsageEventsPageResponse(
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("items") List<UsageEventItem> items,
      @JsonProperty("page") int page,
      @JsonProperty("size") int size,
      @JsonProperty("total_elements") long totalElements,
      @JsonProperty("total_pages") int totalPages) {}

  public record QuotaStatusResponse(
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("decision") String decision,
      @JsonProperty("message") String message,
      @JsonProperty("allowed") boolean allowed,
      @JsonProperty("credit_balance") BigDecimal creditBalance,
      @JsonProperty("breached_dimension") String breachedDimension,
      @JsonProperty("soft_limit") BigDecimal softLimit,
      @JsonProperty("hard_limit") BigDecimal hardLimit,
      @JsonProperty("current_usage") BigDecimal currentUsage,
      @JsonProperty("dimensions") Map<String, DimensionQuotaStatus> dimensions) {}

  public record DimensionQuotaStatus(
      @JsonProperty("soft") BigDecimal soft,
      @JsonProperty("hard") BigDecimal hard,
      @JsonProperty("usage") BigDecimal usage) {}

  public record BillingPeriodItem(
      @JsonProperty("id") String id,
      @JsonProperty("period_start") String periodStart,
      @JsonProperty("period_end") String periodEnd,
      @JsonProperty("total_cost") BigDecimal totalCost,
      @JsonProperty("status") String status) {}

  public record BillingPeriodsResponse(
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("periods") List<BillingPeriodItem> periods) {}
}
