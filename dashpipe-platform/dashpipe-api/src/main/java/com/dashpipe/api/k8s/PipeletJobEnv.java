package com.dashpipe.api.k8s;

/**
 * JSON env payloads injected into pipelet Jobs (architecture §10.3 / pipelet config_merge.py).
 *
 * <p>Pipelets merge layers at runtime; the platform injects decrypted connector/service JSON and
 * step/pipeline deployment + execution overlays.
 */
public record PipeletJobEnv(
    String connectorConfig, String serviceConfig, String deploymentConfig, String executionConfig) {

  public static PipeletJobEnv empty() {
    return new PipeletJobEnv("{}", "{}", "{}", "{}");
  }

  public PipeletJobEnv {
    connectorConfig = blankToEmptyObject(connectorConfig);
    serviceConfig = blankToEmptyObject(serviceConfig);
    deploymentConfig = blankToEmptyObject(deploymentConfig);
    executionConfig = blankToEmptyObject(executionConfig);
  }

  private static String blankToEmptyObject(String raw) {
    if (raw == null || raw.isBlank()) {
      return "{}";
    }
    return raw;
  }
}
