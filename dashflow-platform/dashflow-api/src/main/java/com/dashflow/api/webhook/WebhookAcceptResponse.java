package com.dashflow.api.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WebhookAcceptResponse(
    boolean accepted,
    @JsonProperty("event_id") String eventId,
    @JsonProperty("queued_to") String queuedTo) {}
