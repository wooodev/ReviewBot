package com.reviewbot.review.dto;

import com.reviewbot.review.domain.Review;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID repositoryId,
        int prNumber,
        String prTitle,
        String prAuthor,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getRepository().getId(),
                r.getPrNumber(),
                r.getPrTitle(),
                r.getPrAuthor(),
                r.getStatus().name().toLowerCase(),
                r.getCreatedAt(),
                r.getCompletedAt()
        );
    }
}
