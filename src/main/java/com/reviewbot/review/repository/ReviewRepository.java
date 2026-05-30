package com.reviewbot.review.repository;

import com.reviewbot.review.domain.Review;
import com.reviewbot.review.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findAllByRepositoryUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Review> findAllByRepositoryIdAndRepositoryUserIdOrderByCreatedAtDesc(
            UUID repositoryId, UUID userId, Pageable pageable);

    Optional<Review> findByIdAndRepositoryUserId(UUID id, UUID userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.repository.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.repository.user.id = :userId AND r.status = :status")
    long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ReviewStatus status);
}
