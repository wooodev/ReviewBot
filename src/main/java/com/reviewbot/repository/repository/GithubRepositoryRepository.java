package com.reviewbot.repository.repository;

import com.reviewbot.repository.domain.GithubRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, UUID> {

    List<GithubRepository> findAllByUserIdAndActiveTrue(UUID userId);

    Optional<GithubRepository> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndGithubRepoId(UUID userId, String githubRepoId);

    /** User를 즉시 로딩 — 비동기 리뷰 워커에서 LazyInitializationException 방지 */
    @EntityGraph(attributePaths = {"user"})
    Optional<GithubRepository> findByGithubRepoIdAndActiveTrue(String githubRepoId);
}
