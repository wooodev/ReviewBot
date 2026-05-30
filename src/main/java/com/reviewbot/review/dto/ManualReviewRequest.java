package com.reviewbot.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ManualReviewRequest(
        @NotNull UUID repositoryId,
        @Positive int prNumber
) {}
