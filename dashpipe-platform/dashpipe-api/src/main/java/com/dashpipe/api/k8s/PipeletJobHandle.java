package com.dashpipe.api.k8s;

/** Result of a pipelet Job create request. */
public record PipeletJobHandle(String jobName, String namespace, String status) {

  public static PipeletJobHandle stubbed(PipeletJobRequest request) {
    return new PipeletJobHandle(request.jobName(), request.namespace(), "stubbed");
  }
}
