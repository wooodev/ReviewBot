package com.reviewbot.repository.dto;

import com.reviewbot.repository.domain.RepositoryConfig;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record RepositoryConfigResponse(
        String reviewLanguage,
        String reviewDepth,
        List<String> focusAreas,
        List<String> ignorePatterns,
        LocalDateTime updatedAt
) {
    public static RepositoryConfigResponse from(RepositoryConfig config) {
        return new RepositoryConfigResponse(
                config.getReviewLanguage(),
                config.getReviewDepth(),
                Arrays.asList(config.getFocusAreas()),
                Arrays.asList(config.getIgnorePatterns()),
                config.getUpdatedAt()
        );
    }
}
