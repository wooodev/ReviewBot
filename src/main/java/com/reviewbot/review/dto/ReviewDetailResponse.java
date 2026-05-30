package com.reviewbot.review.dto;

import com.reviewbot.review.domain.Review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewDetailResponse(
        UUID id,
        UUID repositoryId,
        String repositoryFullName,
        int prNumber,
        String prTitle,
        String prAuthor,
        String status,
        String githubReviewId,
        List<CommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static ReviewDetailResponse from(Review r, List<CommentResponse> comments) {
        return new ReviewDetailResponse(
                r.getId(),
                r.getRepository().getId(),
                r.getRepository().getFullName(),
                r.getPrNumber(),
                r.getPrTitle(),
                r.getPrAuthor(),
                r.getStatus().name().toLowerCase(),
                r.getGithubReviewId(),
                comments,
                r.getCreatedAt(),
                r.getCompletedAt()
        );
    }
}
