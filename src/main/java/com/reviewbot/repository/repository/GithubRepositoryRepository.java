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

    /** User를 즉시 로딩 — Webhook 비동기 워커용 */
    @EntityGraph(attributePaths = {"user"})
    Optional<GithubRepository> findByGithubRepoIdAndActiveTrue(String githubRepoId);

    /** User를 즉시 로딩 — 수동 트리거 비동기 워커용 */
    @EntityGraph(attributePaths = {"user"})
    Optional<GithubRepository> findWithUserByIdAndUserId(UUID id, UUID userId);
}
