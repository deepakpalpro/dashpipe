package com.dashflow.connector.spi;

/**
 * Connector plugin SPI (architecture §9.1). Wave 1 registers implementations via Spring beans;
 * PF4J JAR loading is deferred.
 */
public interface Connector {

  /** Connector type identifier (e.g. {@code rest}, {@code storage}). */
  String getType();

  /** SPI version this implementation conforms to. */
  String getSpiVersion();

  /** Initialize with tenant-specific configuration. */
  void configure(ConnectorContext context, ConnectorConfig config);

  /** Validate connectivity (used by {@code POST /connectors/{id}/test}). */
  ConnectionTestResult testConnection();

  /** Read from the external system. */
  ConnectorResponse read(ConnectorRequest request);

  /** Write to the external system. */
  ConnectorResponse write(ConnectorRequest request);

  /** Release resources. */
  void close();
}
