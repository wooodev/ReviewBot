package com.reviewbot.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WebhookCreationResponse(
        @JsonProperty("id") long id,
        @JsonProperty("active") boolean active
) {}
