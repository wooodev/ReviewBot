package com.reviewbot.repository.repository;

import com.reviewbot.repository.domain.RepositoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositoryConfigRepository extends JpaRepository<RepositoryConfig, UUID> {

    Optional<RepositoryConfig> findByRepositoryId(UUID repositoryId);
}
