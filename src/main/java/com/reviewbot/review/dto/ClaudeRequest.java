package com.reviewbot.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages
) {
    public record Message(String role, String content) {}
}
