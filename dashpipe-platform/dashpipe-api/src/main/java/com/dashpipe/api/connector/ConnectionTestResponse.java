package com.dashpipe.api.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Architecture §3.3 test-connection response. */
public record ConnectionTestResponse(
    boolean success,
    @JsonProperty("latency_ms") long latencyMs,
    String message,
    @JsonProperty("tested_at") Instant testedAt) {}
