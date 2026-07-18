package com.dashpipe.api.k8s;

/**
 * Kubernetes label keys for pipelet Jobs / pods.
 *
 * <p>Prefix {@code dashpipe.io/} is the Dashpipe domain label namespace (replaces legacy {@code
 * pipeline.platform/}).
 */
public final class PipeletK8sLabels {

  public static final String PREFIX = "dashpipe.io/";

  public static final String TENANT_ID = PREFIX + "tenant_id";
  public static final String TENANT = PREFIX + "tenant";
  public static final String PIPELINE_ID = PREFIX + "pipeline_id";
  public static final String PIPELINE = PREFIX + "pipeline";
  public static final String EXECUTION_ID = PREFIX + "execution_id";
  public static final String PIPELET_ID = PREFIX + "pipelet_id";
  public static final String PIPELET = PREFIX + "pipelet";
  public static final String STAGE_ORDER = PREFIX + "stage_order";

  private PipeletK8sLabels() {}
}
