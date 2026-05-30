package com.reviewbot.review.dto;

public record ReviewStatsResponse(
        long totalReviews,
        long completedReviews,
        long failedReviews,
        long totalComments,
        long criticalIssues
) {}
