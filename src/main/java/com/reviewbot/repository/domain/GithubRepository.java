package com.reviewbot.repository.domain;

import com.reviewbot.auth.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repositories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GithubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repo_id", nullable = false)
    private String githubRepoId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "webhook_id")
    private String webhookId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static GithubRepository of(User user, String githubRepoId,
                                      String fullName, String webhookId) {
        GithubRepository repo = new GithubRepository();
        repo.user = user;
        repo.githubRepoId = githubRepoId;
        repo.fullName = fullName;
        repo.webhookId = webhookId;
        return repo;
    }

    public void deactivate() {
        this.active = false;
        this.webhookId = null;
    }
}
