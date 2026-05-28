package com.reviewbot.repository.dto;

import com.reviewbot.repository.domain.GithubRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        String fullName,
        boolean active,
        LocalDateTime createdAt
) {
    public static RepositoryResponse from(GithubRepository repo) {
        return new RepositoryResponse(
                repo.getId(),
                repo.getFullName(),
                repo.isActive(),
                repo.getCreatedAt()
        );
    }
}
