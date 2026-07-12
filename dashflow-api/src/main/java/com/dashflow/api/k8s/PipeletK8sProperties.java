package com.dashflow.api.k8s;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pipeline.k8s")
public class PipeletK8sProperties {

  /** When true, {@link Fabric8PipeletJobClient} is the active {@link PipeletJobClient}. */
  private boolean enabled = false;

  /** Image pull policy for pipelet Jobs (Rancher/local: IfNotPresent). */
  private String imagePullPolicy = "IfNotPresent";

  /**
   * Default image pattern; {@code {pipeletId}} is replaced. Example: {@code
   * dashflow/{pipeletId}:local}
   */
  private String defaultImagePattern = "dashflow/{pipeletId}:local";

  /** Optional per-pipelet image overrides (pipeletId → image ref). */
  private Map<String, String> images = new HashMap<>();

  /** CPU request for the pipelet container. */
  private String cpuRequest = "100m";

  /** Memory request for the pipelet container. */
  private String memoryRequest = "128Mi";

  /** CPU limit for the pipelet container. */
  private String cpuLimit = "500m";

  /** Memory limit for the pipelet container. */
  private String memoryLimit = "512Mi";

  /** TTL seconds after Job finishes (cleanup). */
  private int ttlSecondsAfterFinished = 600;

  /** Job backoff limit. */
  private int backoffLimit = 2;

  /**
   * When true, ensure the tenant namespace exists before creating the Job (create if missing).
   */
  private boolean createNamespace = true;

  /** Poll interval for Job success/failure reconciliation when stub-stage-worker is false. */
  private long jobStatusPollIntervalMs = 2000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getImagePullPolicy() {
    return imagePullPolicy;
  }

  public void setImagePullPolicy(String imagePullPolicy) {
    this.imagePullPolicy = imagePullPolicy;
  }

  public String getDefaultImagePattern() {
    return defaultImagePattern;
  }

  public void setDefaultImagePattern(String defaultImagePattern) {
    this.defaultImagePattern = defaultImagePattern;
  }

  public Map<String, String> getImages() {
    return images;
  }

  public void setImages(Map<String, String> images) {
    this.images = images != null ? images : new HashMap<>();
  }

  public String getCpuRequest() {
    return cpuRequest;
  }

  public void setCpuRequest(String cpuRequest) {
    this.cpuRequest = cpuRequest;
  }

  public String getMemoryRequest() {
    return memoryRequest;
  }

  public void setMemoryRequest(String memoryRequest) {
    this.memoryRequest = memoryRequest;
  }

  public String getCpuLimit() {
    return cpuLimit;
  }

  public void setCpuLimit(String cpuLimit) {
    this.cpuLimit = cpuLimit;
  }

  public String getMemoryLimit() {
    return memoryLimit;
  }

  public void setMemoryLimit(String memoryLimit) {
    this.memoryLimit = memoryLimit;
  }

  public int getTtlSecondsAfterFinished() {
    return ttlSecondsAfterFinished;
  }

  public void setTtlSecondsAfterFinished(int ttlSecondsAfterFinished) {
    this.ttlSecondsAfterFinished = ttlSecondsAfterFinished;
  }

  public int getBackoffLimit() {
    return backoffLimit;
  }

  public void setBackoffLimit(int backoffLimit) {
    this.backoffLimit = backoffLimit;
  }

  public boolean isCreateNamespace() {
    return createNamespace;
  }

  public void setCreateNamespace(boolean createNamespace) {
    this.createNamespace = createNamespace;
  }

  public long getJobStatusPollIntervalMs() {
    return jobStatusPollIntervalMs;
  }

  public void setJobStatusPollIntervalMs(long jobStatusPollIntervalMs) {
    this.jobStatusPollIntervalMs = jobStatusPollIntervalMs;
  }

  public String resolveImage(String pipeletId) {
    if (pipeletId != null && images.containsKey(pipeletId)) {
      return images.get(pipeletId);
    }
    String id = pipeletId == null ? "unknown" : pipeletId;
    String pattern =
        defaultImagePattern == null || defaultImagePattern.isBlank()
            ? "dashflow/{pipeletId}:local"
            : defaultImagePattern;
    return pattern.replace("{pipeletId}", id);
  }

  /** DNS-1123 namespace: {@code tenant-{tenantId}} lowercased. */
  public static String namespaceForTenant(String tenantId) {
    String raw = "tenant-" + (tenantId == null || tenantId.isBlank() ? "unknown" : tenantId);
    return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
  }

  /** DNS-1123 Job name (lowercase, max 63). */
  public static String sanitizeJobName(String jobName) {
    String raw = jobName == null || jobName.isBlank() ? "pipelet-job" : jobName;
    String sanitized = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.-]", "-");
    if (sanitized.length() > 63) {
      sanitized = sanitized.substring(0, 63);
    }
    while (sanitized.endsWith("-") || sanitized.endsWith(".")) {
      sanitized = sanitized.substring(0, sanitized.length() - 1);
    }
    return sanitized.isBlank() ? "pipelet-job" : sanitized;
  }
}
