package com.dashflow.spi.platform;

/**
 * Identifies which cloud/technology adapters are active for this deployment.
 *
 * <p>Example Azure assembly: broker=servicebus, runtime=kubernetes, database=mysql,
 * observability=azure-monitor.
 */
public record PlatformAssembly(
    String broker, String runtime, String database, String observability) {

  public static final String BROKER_RABBITMQ = "rabbitmq";
  public static final String BROKER_SERVICEBUS = "servicebus";
  public static final String RUNTIME_KUBERNETES = "kubernetes";
  public static final String DATABASE_MYSQL = "mysql";
  public static final String DATABASE_POSTGRESQL = "postgresql";
  public static final String OBS_PROMETHEUS = "prometheus";
  public static final String OBS_AZURE_MONITOR = "azure-monitor";

  public static PlatformAssembly localDefaults() {
    return new PlatformAssembly(
        BROKER_RABBITMQ, RUNTIME_KUBERNETES, DATABASE_MYSQL, OBS_PROMETHEUS);
  }

  public static PlatformAssembly azureDefaults() {
    return new PlatformAssembly(
        BROKER_SERVICEBUS, RUNTIME_KUBERNETES, DATABASE_MYSQL, OBS_AZURE_MONITOR);
  }
}
