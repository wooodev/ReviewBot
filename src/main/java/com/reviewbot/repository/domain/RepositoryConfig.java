package com.reviewbot.repository.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repository_configs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RepositoryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GithubRepository repository;

    @Column(name = "review_language", nullable = false)
    private String reviewLanguage = "ko";

    @Column(name = "review_depth", nullable = false)
    private String reviewDepth = "detailed";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "focus_areas", columnDefinition = "text[]", nullable = false)
    private String[] focusAreas = {"security", "performance", "style", "bug"};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ignore_patterns", columnDefinition = "text[]", nullable = false)
    private String[] ignorePatterns = {"*.md", "*.json", "*.lock"};

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static RepositoryConfig defaultConfig(GithubRepository repository) {
        RepositoryConfig config = new RepositoryConfig();
        config.repository = repository;
        return config;
    }

    public void update(String reviewLanguage, String reviewDepth,
                       String[] focusAreas, String[] ignorePatterns) {
        this.reviewLanguage = reviewLanguage;
        this.reviewDepth = reviewDepth;
        this.focusAreas = focusAreas;
        this.ignorePatterns = ignorePatterns;
        this.updatedAt = LocalDateTime.now();
    }
}
