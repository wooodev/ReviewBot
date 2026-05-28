package com.reviewbot.repository.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRepositoryRequest(
        @NotBlank String githubRepoId,
        @NotBlank String fullName
) {}
