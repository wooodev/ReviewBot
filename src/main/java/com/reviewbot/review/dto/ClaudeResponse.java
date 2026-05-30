package com.reviewbot.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeResponse(
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason
) {
    public record ContentBlock(String type, String text) {}

    public String extractText() {
        if (content == null) return "[]";
        return content.stream()
                .filter(b -> "text".equals(b.type()))
                .map(ContentBlock::text)
                .findFirst()
                .orElse("[]");
    }
}
