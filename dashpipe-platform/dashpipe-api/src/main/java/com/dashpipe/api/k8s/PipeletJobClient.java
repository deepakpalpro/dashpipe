package com.dashpipe.api.k8s;

/**
 * Creates ephemeral pipelet work units (architecture §10.3 Jobs).
 *
 * <p>Local/default: {@link StubPipeletJobClient}. Optional Kind/cluster impl can replace this bean.
 */
public interface PipeletJobClient {

  PipeletJobHandle create(PipeletJobRequest request);

  /**
   * Deletes Jobs for an execution so a force-stop can free the cluster and allow a fresh run.
   *
   * @return number of Jobs deleted (best-effort; stubs may return 0)
   */
  int deleteByExecution(String tenantId, String executionId);
}
