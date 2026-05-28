package com.reviewbot.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "github_id", unique = true, nullable = false)
    private String githubId;

    @Column(name = "github_login", nullable = false)
    private String githubLogin;

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "access_token")
    private String accessToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.FREE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static User of(String githubId, String githubLogin, String email,
                          String avatarUrl, String accessToken) {
        User user = new User();
        user.githubId = githubId;
        user.githubLogin = githubLogin;
        user.email = email;
        user.avatarUrl = avatarUrl;
        user.accessToken = accessToken;
        return user;
    }

    public void update(String githubLogin, String email, String avatarUrl, String accessToken) {
        this.githubLogin = githubLogin;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.accessToken = accessToken;
    }
}
