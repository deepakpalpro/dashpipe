package com.dashpipe.api.config;

import com.dashpipe.spi.platform.PlatformAssembly;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dashpipe.platform")
public class PlatformProperties {

  /** Active broker adapter: rabbitmq | servicebus */
  private String broker = PlatformAssembly.BROKER_RABBITMQ;

  /** Job runtime: kubernetes (AKS / EKS / GKE / local) */
  private String runtime = PlatformAssembly.RUNTIME_KUBERNETES;

  /** Metadata store dialect: mysql | postgresql (postgresql adapter later) */
  private String database = PlatformAssembly.DATABASE_MYSQL;

  /** Observability sink: prometheus | azure-monitor */
  private String observability = PlatformAssembly.OBS_PROMETHEUS;

  private final ServiceBus servicebus = new ServiceBus();

  public String getBroker() {
    return broker;
  }

  public void setBroker(String broker) {
    this.broker = broker;
  }

  public String getRuntime() {
    return runtime;
  }

  public void setRuntime(String runtime) {
    this.runtime = runtime;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getObservability() {
    return observability;
  }

  public void setObservability(String observability) {
    this.observability = observability;
  }

  public ServiceBus getServicebus() {
    return servicebus;
  }

  public PlatformAssembly toAssembly() {
    return new PlatformAssembly(broker, runtime, database, observability);
  }

  public static class ServiceBus {
    private String connectionString = "";
    private String fullyQualifiedNamespace = "";

    public String getConnectionString() {
      return connectionString;
    }

    public void setConnectionString(String connectionString) {
      this.connectionString = connectionString;
    }

    public String getFullyQualifiedNamespace() {
      return fullyQualifiedNamespace;
    }

    public void setFullyQualifiedNamespace(String fullyQualifiedNamespace) {
      this.fullyQualifiedNamespace = fullyQualifiedNamespace;
    }
  }
}
