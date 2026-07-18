package com.dashpipe.connector.spi;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Runtime context passed to a connector instance (architecture §9.2). {@code serviceResolver} and
 * {@code meterRegistry} may be null in early Wave 1 unit tests.
 */
public record ConnectorContext(
    String tenantId,
    String connectorId,
    String executionId,
    ServiceResolver serviceResolver,
    MeterRegistry meterRegistry) {}
