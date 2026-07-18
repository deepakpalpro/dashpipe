package com.dashflow.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** W4-US04: §7.3 index naming. */
class PipelineLogIndexNamesTest {

  @Test
  void forTenantAndInstant_matchesArchitecturePattern() {
    Instant day = Instant.parse("2026-07-09T12:00:00Z");
    assertThat(PipelineLogIndexNames.forTenantAndInstant("T001", day))
        .isEqualTo("pipeline-logs-t001-2026.07.09");
  }

  @Test
  void kibanaPattern_isTenantWildcard() {
    assertThat(PipelineLogIndexNames.kibanaPattern("Acme_Tenant"))
        .isEqualTo("pipeline-logs-acme_tenant-*");
  }

  @Test
  void blankTenant_usesUnknown() {
    assertThat(PipelineLogIndexNames.forTenantAndInstant("  ", Instant.parse("2026-01-01T00:00:00Z")))
        .isEqualTo("pipeline-logs-unknown-2026.01.01");
  }
}
