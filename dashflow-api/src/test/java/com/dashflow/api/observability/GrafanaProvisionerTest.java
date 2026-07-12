package com.dashflow.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W4-US06: Grafana provision stub. */
class GrafanaProvisionerTest {

  private StubGrafanaClient client;
  private GrafanaProvisioner provisioner;

  @BeforeEach
  void setUp() {
    client = new StubGrafanaClient();
    provisioner = new GrafanaProvisioner(client);
  }

  @Test
  void provision_createsOrgAndUpsertsDashboard() {
    GrafanaProvisionResult result = provisioner.provisionTenant("T001", "Acme");

    assertThat(result.tenantId()).isEqualTo("T001");
    assertThat(result.orgId()).isEqualTo(1L);
    assertThat(result.orgName()).contains("Acme").contains("T001");
    assertThat(result.dashboardUid()).isEqualTo("dash-org-1");

    assertThat(client.calls())
        .extracting(StubGrafanaClient.ProvisionCall::action)
        .containsExactly("createOrg", "upsertDashboard");
    assertThat(client.orgsByTenant()).containsKey("T001");
  }

  @Test
  void provision_isIdempotentForOrg() {
    provisioner.provisionTenant("T001", "Acme");
    GrafanaProvisionResult second = provisioner.provisionTenant("T001", "Acme");

    assertThat(second.orgId()).isEqualTo(1L);
    assertThat(client.orgsByTenant()).hasSize(1);
  }
}
