-- V1: Initial schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 사용자
CREATE TABLE users (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    github_id    VARCHAR(100) UNIQUE NOT NULL,
    github_login VARCHAR(100) NOT NULL,
    email        VARCHAR(255),
    avatar_url   VARCHAR(500),
    access_token TEXT,
    plan         VARCHAR(20)  NOT NULL DEFAULT 'free',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 연동된 저장소
CREATE TABLE repositories (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id VARCHAR(100) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    webhook_id     VARCHAR(100),
    is_active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, github_repo_id)
);

-- 저장소별 리뷰 설정
CREATE TABLE repository_configs (
    id               UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id    UUID      NOT NULL UNIQUE REFERENCES repositories(id) ON DELETE CASCADE,
    review_language  VARCHAR(10)  NOT NULL DEFAULT 'ko',
    review_depth     VARCHAR(20)  NOT NULL DEFAULT 'detailed',
    focus_areas      TEXT[]    NOT NULL DEFAULT '{"security","performance","style","bug"}',
    ignore_patterns  TEXT[]    NOT NULL DEFAULT '{"*.md","*.json","*.lock"}',
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- PR 리뷰 이력
CREATE TABLE reviews (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id    UUID        NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    pr_number        INTEGER     NOT NULL,
    pr_title         VARCHAR(500),
    pr_author        VARCHAR(100),
    status           VARCHAR(20)  NOT NULL DEFAULT 'pending',
    github_review_id VARCHAR(100),
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMP
);

-- 리뷰 댓글 상세
CREATE TABLE review_comments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id   UUID        NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path   VARCHAR(500) NOT NULL,
    line_number INTEGER,
    comment     TEXT        NOT NULL,
    severity    VARCHAR(20),
    category    VARCHAR(50),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_repositories_user_id ON repositories(user_id);
CREATE INDEX idx_reviews_repository_id ON reviews(repository_id);
CREATE INDEX idx_reviews_status ON reviews(status);
CREATE INDEX idx_review_comments_review_id ON review_comments(review_id);
