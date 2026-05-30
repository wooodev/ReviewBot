package com.reviewbot.review.domain;

import com.reviewbot.repository.domain.GithubRepository;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GithubRepository repository;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    @Column(name = "pr_title")
    private String prTitle;

    @Column(name = "pr_author")
    private String prAuthor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "github_review_id")
    private String githubReviewId;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReviewComment> comments = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static Review of(GithubRepository repository, int prNumber, String prTitle, String prAuthor) {
        Review review = new Review();
        review.repository = repository;
        review.prNumber = prNumber;
        review.prTitle = prTitle;
        review.prAuthor = prAuthor;
        review.status = ReviewStatus.PROCESSING;
        return review;
    }

    public void complete(String githubReviewId) {
        this.status = ReviewStatus.COMPLETED;
        this.githubReviewId = githubReviewId;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = ReviewStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}
