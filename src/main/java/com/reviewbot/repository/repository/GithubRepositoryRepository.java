package com.reviewbot.repository.repository;

import com.reviewbot.repository.domain.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, UUID> {

    List<GithubRepository> findAllByUserIdAndActiveTrue(UUID userId);

    Optional<GithubRepository> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndGithubRepoId(UUID userId, String githubRepoId);
}
