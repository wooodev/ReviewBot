package com.reviewbot.github.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.github.dto.WebhookPayload;
import com.reviewbot.github.service.WebhookVerifier;
import com.reviewbot.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final Set<String> HANDLED_ACTIONS = Set.of("opened", "synchronize");

    private final WebhookVerifier webhookVerifier;
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    @PostMapping("/github")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String rawPayload) throws JsonProcessingException {

        // 1. HMAC-SHA256 서명 검증 (위조 방지)
        webhookVerifier.verify(rawPayload, signature);

        // 2. PR 이벤트만 처리
        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok().build();
        }

        WebhookPayload payload = objectMapper.readValue(rawPayload, WebhookPayload.class);
        log.info("[Webhook] action={} repo={} PR=#{}",
                payload.getAction(),
                payload.getRepository().getFullName(),
                payload.getPullRequest() != null ? payload.getPullRequest().getNumber() : "N/A");

        // 3. opened / synchronize 이벤트에서 비동기 리뷰 트리거
        //    GitHub은 10초 내 응답 필요 → 즉시 200 반환 후 백그라운드 처리
        if (HANDLED_ACTIONS.contains(payload.getAction())) {
            reviewService.triggerReviewAsync(payload);
        }

        return ResponseEntity.ok().build();
    }
}
