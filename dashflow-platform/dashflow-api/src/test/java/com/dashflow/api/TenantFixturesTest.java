package com.dashflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.dashflow.api.support.TenantFixtures;
import org.junit.jupiter.api.Test;

class TenantFixturesTest {

  @Test
  void loadsT001() {
    JsonNode tenant = TenantFixtures.loadT001();
    assertThat(tenant.get("id").asText()).isEqualTo(TenantFixtures.T001);
    assertThat(tenant.get("slug").asText()).isEqualTo("demo");
    assertThat(tenant.get("name").asText()).isEqualTo("Demo Tenant");
    assertThat(tenant.get("status").asText()).isEqualTo("active");
  }
}
