package com.reviewbot.review.service;

import com.reviewbot.github.dto.WebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReviewService {

    /**
     * Phase 3에서 구현: PR diff 추출 → Claude API 분석 → GitHub 댓글 등록
     */
    @Async("reviewTaskExecutor")
    public void triggerReviewAsync(WebhookPayload payload) {
        log.info("[ReviewService] Review triggered — repo: {}, PR #{}",
                payload.getRepository().getFullName(),
                payload.getPullRequest().getNumber());
    }
}
