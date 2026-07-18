package com.dashpipe.api.observability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * In-memory Grafana client stub for Wave 4 (W4-US06). Records provision calls for tests; no live
 * Grafana required.
 */
@Component
public class StubGrafanaClient implements GrafanaClient {

  private final AtomicLong orgSeq = new AtomicLong(1);
  private final ConcurrentHashMap<String, GrafanaOrg> orgsByTenant = new ConcurrentHashMap<>();
  private final List<ProvisionCall> calls = new ArrayList<>();

  @Override
  public synchronized GrafanaOrg createOrg(String tenantId, String orgName) {
    GrafanaOrg existing = orgsByTenant.get(tenantId);
    if (existing != null) {
      calls.add(new ProvisionCall("createOrg", tenantId, existing.orgId(), null));
      return existing;
    }
    GrafanaOrg created = new GrafanaOrg(orgSeq.getAndIncrement(), orgName);
    orgsByTenant.put(tenantId, created);
    calls.add(new ProvisionCall("createOrg", tenantId, created.orgId(), null));
    return created;
  }

  @Override
  public synchronized String upsertDashboard(long orgId, String dashboardJson) {
    String uid = "dash-org-" + orgId;
    calls.add(new ProvisionCall("upsertDashboard", null, orgId, uid));
    return uid;
  }

  public synchronized List<ProvisionCall> calls() {
    return List.copyOf(calls);
  }

  public Map<String, GrafanaOrg> orgsByTenant() {
    return Map.copyOf(orgsByTenant);
  }

  public synchronized void clear() {
    orgsByTenant.clear();
    calls.clear();
    orgSeq.set(1);
  }

  public record ProvisionCall(String action, String tenantId, Long orgId, String dashboardUid) {}
}
