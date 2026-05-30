package com.reviewbot.review.domain;

import com.reviewbot.review.dto.AiReviewComment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private Category category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReviewComment of(Review review, AiReviewComment ai) {
        ReviewComment rc = new ReviewComment();
        rc.review = review;
        rc.filePath = ai.filePath();
        rc.lineNumber = ai.lineNumber();
        rc.comment = ai.comment();
        rc.severity = parseSeverity(ai.severity());
        rc.category = parseCategory(ai.category());
        return rc;
    }

    private static Severity parseSeverity(String value) {
        try { return value != null ? Severity.valueOf(value.toUpperCase()) : null; }
        catch (IllegalArgumentException e) { return null; }
    }

    private static Category parseCategory(String value) {
        try { return value != null ? Category.valueOf(value.toUpperCase()) : null; }
        catch (IllegalArgumentException e) { return null; }
    }
}
