package com.reviewbot.github.service;

import com.reviewbot.github.dto.WebhookCreationResponse;
import com.reviewbot.repository.dto.GitHubRepoResponse;
import com.reviewbot.review.dto.AiReviewComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    @Qualifier("githubWebClient")
    private final WebClient githubWebClient;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    public List<GitHubRepoResponse> getUserRepositories(String accessToken) {
        return withToken(accessToken)
                .get()
                .uri("/user/repos?type=owner&sort=updated&per_page=100")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GitHubRepoResponse>>() {})
                .block();
    }

    public String createWebhook(String accessToken, String repoFullName) {
        Map<String, Object> body = Map.of(
                "name", "web",
                "active", true,
                "events", List.of("pull_request"),
                "config", Map.of(
                        "url", appBaseUrl + "/webhook/github",
                        "content_type", "json",
                        "secret", webhookSecret,
                        "insecure_ssl", "0"
                )
        );

        WebhookCreationResponse response = withToken(accessToken)
                .post()
                .uri("/repos/{repo}/hooks", repoFullName)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(WebhookCreationResponse.class)
                .block();

        return response != null ? String.valueOf(response.id()) : null;
    }

    public void deleteWebhook(String accessToken, String repoFullName, String webhookId) {
        try {
            withToken(accessToken)
                    .delete()
                    .uri("/repos/{repo}/hooks/{hookId}", repoFullName, webhookId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            // 이미 삭제된 webhook이면 무시
            log.warn("Webhook {} not found on GitHub, skipping delete", webhookId);
        }
    }

    public String getPullRequestDiff(String accessToken, String repoFullName, int prNumber) {
        return withToken(accessToken)
                .get()
                .uri("/repos/{repo}/pulls/{number}", repoFullName, prNumber)
                .header("Accept", "application/vnd.github.v3.diff")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * GitHub PR에 AI 리뷰 댓글 등록.
     * lineNumber가 있는 코멘트는 인라인, 없는 코멘트는 본문에 포함한다.
     *
     * @param headSha PR head commit SHA. null이면 인라인 댓글 없이 본문만 등록.
     * @return GitHub review ID
     */
    @SuppressWarnings("unchecked")
    public String postReviewComments(String accessToken, String repoFullName, int prNumber,
                                     String headSha, List<AiReviewComment> comments) {
        List<Map<String, Object>> inlineComments = new ArrayList<>();
        List<AiReviewComment> bodyOnlyComments   = new ArrayList<>();

        for (AiReviewComment c : comments) {
            if (headSha != null && c.lineNumber() != null && c.lineNumber() > 0) {
                inlineComments.add(Map.of(
                        "path", c.filePath(),
                        "line", c.lineNumber(),
                        "body", formatInlineComment(c)
                ));
            } else {
                bodyOnlyComments.add(c);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("body", buildReviewBody(bodyOnlyComments, comments.size()));
        body.put("event", "COMMENT");
        if (headSha != null) body.put("commit_id", headSha);
        if (!inlineComments.isEmpty()) body.put("comments", inlineComments);

        Map<String, Object> response = withToken(accessToken)
                .post()
                .uri("/repos/{repo}/pulls/{number}/reviews", repoFullName, prNumber)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return response != null ? String.valueOf(response.get("id")) : null;
    }

    private String formatInlineComment(AiReviewComment c) {
        String badge = switch (c.severity() != null ? c.severity().toLowerCase() : "info") {
            case "critical" -> "🔴 **Critical**";
            case "warning"  -> "🟡 **Warning**";
            default         -> "🔵 **Info**";
        };
        return "%s `[%s]`\n\n%s".formatted(badge, c.category(), c.comment());
    }

    private String buildReviewBody(List<AiReviewComment> generalComments, int totalCount) {
        StringBuilder sb = new StringBuilder("## 🤖 ReviewBot AI 코드 리뷰\n\n");
        sb.append("> 총 **%d**개의 리뷰 코멘트가 생성되었습니다.\n\n".formatted(totalCount));

        if (!generalComments.isEmpty()) {
            sb.append("### 📋 일반 코멘트\n\n");
            for (AiReviewComment c : generalComments) {
                sb.append("- **[%s]** `%s` — %s\n".formatted(
                        c.severity(), c.filePath(), c.comment()));
            }
        }
        return sb.toString();
    }

    private WebClient withToken(String accessToken) {
        return githubWebClient.mutate()
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }
}
