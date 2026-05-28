package com.reviewbot.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RepositoryConfigRequest(
        @NotBlank String reviewLanguage,
        @NotBlank String reviewDepth,
        @NotNull List<String> focusAreas,
        @NotNull List<String> ignorePatterns
) {}
