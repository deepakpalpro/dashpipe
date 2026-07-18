package com.dashpipe.api.billing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Architecture §3.5 usage and billing endpoints. */
@RestController
@RequestMapping("/api/v1/tenants/{id}")
public class BillingController {

  private final BillingQueryService billingQueryService;

  public BillingController(BillingQueryService billingQueryService) {
    this.billingQueryService = billingQueryService;
  }

  @GetMapping("/usage")
  public BillingDtos.UsageSummaryResponse usage(
      @PathVariable("id") String tenantId,
      @RequestParam(value = "period", defaultValue = "current") String period) {
    // Wave 5: only current calendar month
    return billingQueryService.usageSummary(tenantId);
  }

  @GetMapping("/usage/events")
  public BillingDtos.UsageEventsPageResponse usageEvents(
      @PathVariable("id") String tenantId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {
    return billingQueryService.usageEvents(tenantId, page, size);
  }

  @GetMapping("/quota")
  public BillingDtos.QuotaStatusResponse quota(@PathVariable("id") String tenantId) {
    return billingQueryService.quotaStatus(tenantId);
  }

  @GetMapping("/billing/periods")
  public BillingDtos.BillingPeriodsResponse billingPeriods(@PathVariable("id") String tenantId) {
    return billingQueryService.billingPeriods(tenantId);
  }
}
