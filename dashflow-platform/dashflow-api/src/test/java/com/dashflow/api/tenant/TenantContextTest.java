package com.dashflow.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  @Test
  void holdsTenantIdForCurrentThreadOrRequest() {
    assertThat(TenantContext.getTenantId()).isNull();

    TenantContext.setTenantId("T001");
    assertThat(TenantContext.getTenantId()).isEqualTo("T001");

    TenantContext.clear();
    assertThat(TenantContext.getTenantId()).isNull();
  }
}
