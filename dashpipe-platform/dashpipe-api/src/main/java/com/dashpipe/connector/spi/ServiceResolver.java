package com.dashpipe.connector.spi;

/**
 * Resolves merged tenant service config (architecture §9.3). Wired for real use in later waves;
 * Rest connector does not require it in W1-US05.
 */
public interface ServiceResolver {

  <T> T resolve(String tenantId, String serviceType, String vendor, Class<T> configClass);
}
