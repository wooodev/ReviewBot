package com.reviewbot.github.service;

import com.reviewbot.github.dto.WebhookCreationResponse;
import com.reviewbot.repository.dto.GitHubRepoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    private WebClient withToken(String accessToken) {
        return githubWebClient.mutate()
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }
}
