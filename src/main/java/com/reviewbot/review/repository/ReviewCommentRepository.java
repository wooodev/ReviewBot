package com.reviewbot.review.repository;

import com.reviewbot.review.domain.ReviewComment;
import com.reviewbot.review.domain.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, UUID> {

    List<ReviewComment> findAllByReviewId(UUID reviewId);

    @Query("SELECT COUNT(rc) FROM ReviewComment rc WHERE rc.review.repository.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(rc) FROM ReviewComment rc WHERE rc.review.repository.user.id = :userId AND rc.severity = :severity")
    long countByUserIdAndSeverity(@Param("userId") UUID userId, @Param("severity") Severity severity);
}
