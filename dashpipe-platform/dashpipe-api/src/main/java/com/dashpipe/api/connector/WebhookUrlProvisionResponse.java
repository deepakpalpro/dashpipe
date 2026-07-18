package com.dashpipe.api.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Architecture §3.3 provision webhook URL response. */
public record WebhookUrlProvisionResponse(
    @JsonProperty("webhook_url") String webhookUrl,
    @JsonProperty("signing_secret") String signingSecret,
    @JsonProperty("signature_header") String signatureHeader,
    @JsonProperty("created_at") Instant createdAt) {}
