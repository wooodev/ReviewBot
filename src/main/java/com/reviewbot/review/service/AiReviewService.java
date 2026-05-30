package com.reviewbot.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.repository.domain.RepositoryConfig;
import com.reviewbot.review.dto.AiReviewComment;
import com.reviewbot.review.dto.ClaudeRequest;
import com.reviewbot.review.dto.ClaudeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private static final int MAX_DIFF_LENGTH = 80_000;

    @Qualifier("claudeWebClient")
    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.claude.model}")
    private String model;

    @Value("${ai.claude.max-tokens}")
    private int maxTokens;

    public List<AiReviewComment> analyzeCode(String diff, RepositoryConfig config) {
        String filteredDiff = filterDiff(diff, config);
        if (filteredDiff == null || filteredDiff.isBlank()) {
            log.info("[AiReview] Diff is empty after filtering — skipping review");
            return Collections.emptyList();
        }

        String truncatedDiff = truncate(filteredDiff);
        String systemPrompt  = buildSystemPrompt(config);

        ClaudeRequest request = new ClaudeRequest(
                model, maxTokens, systemPrompt,
                List.of(new ClaudeRequest.Message("user", truncatedDiff))
        );

        ClaudeResponse response = claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .block();

        if (response == null) {
            log.warn("[AiReview] Claude API returned null");
            return Collections.emptyList();
        }

        return parseComments(response.extractText());
    }

    // ── 프롬프트 ──────────────────────────────────────────────────────────────

    private String buildSystemPrompt(RepositoryConfig config) {
        String focusAreas = config != null && config.getFocusAreas() != null
                ? String.join(", ", config.getFocusAreas())
                : "security, performance, style, bug";
        String language = config != null && "en".equals(config.getReviewLanguage())
                ? "English" : "한국어";

        return """
                당신은 10년 경력의 시니어 소프트웨어 엔지니어입니다.
                주어진 코드 변경사항(git diff)을 분석하고 코드 리뷰를 제공하세요.

                리뷰 집중 항목: %s
                응답 언어: %s

                규칙:
                1. 실제로 문제가 있는 부분만 지적하세요. 트리비얼한 스타일 지적은 최소화하세요.
                2. 각 코멘트에는 왜 문제인지, 어떻게 개선해야 하는지를 명확히 서술하세요.
                3. severity는 critical(버그/보안 취약점), warning(잠재적 문제), info(개선 제안) 중 하나입니다.
                4. 반드시 아래 JSON 배열 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

                [
                  {
                    "filePath": "파일 경로",
                    "lineNumber": 라인번호 또는 null,
                    "severity": "critical|warning|info",
                    "category": "security|performance|style|bug",
                    "comment": "리뷰 내용"
                  }
                ]
                """.formatted(focusAreas, language);
    }

    // ── diff 전처리 ──────────────────────────────────────────────────────────

    private String filterDiff(String diff, RepositoryConfig config) {
        if (diff == null || diff.isBlank()) return diff;
        if (config == null || config.getIgnorePatterns() == null) return diff;

        List<PathMatcher> matchers = Arrays.stream(config.getIgnorePatterns())
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .toList();

        String[] sections = diff.split("(?=diff --git )");
        StringBuilder result = new StringBuilder();

        for (String section : sections) {
            if (section.isBlank()) continue;
            String filePath = extractFilePath(section);
            if (filePath == null || !matchesAny(filePath, matchers)) {
                result.append(section);
            } else {
                log.debug("[AiReview] Ignoring file: {}", filePath);
            }
        }
        return result.toString();
    }

    private String extractFilePath(String section) {
        // "diff --git a/path b/path" 에서 b/path 추출
        String firstLine = section.split("\n")[0];
        int bIdx = firstLine.lastIndexOf(" b/");
        return bIdx >= 0 ? firstLine.substring(bIdx + 3).trim() : null;
    }

    private boolean matchesAny(String filePath, List<PathMatcher> matchers) {
        return matchers.stream().anyMatch(m -> {
            try { return m.matches(Paths.get(filePath)); }
            catch (Exception e) { return false; }
        });
    }

    private String truncate(String diff) {
        if (diff.length() <= MAX_DIFF_LENGTH) return diff;
        log.warn("[AiReview] Diff truncated from {} to {} chars", diff.length(), MAX_DIFF_LENGTH);
        return diff.substring(0, MAX_DIFF_LENGTH) + "\n\n[... diff truncated ...]";
    }

    // ── 응답 파싱 ─────────────────────────────────────────────────────────────

    private List<AiReviewComment> parseComments(String text) {
        try {
            // JSON 배열 부분만 추출 (Claude가 마크다운 블록으로 감싸는 경우 대비)
            int start = text.indexOf('[');
            int end   = text.lastIndexOf(']');
            if (start < 0 || end < 0 || end <= start) {
                log.warn("[AiReview] Cannot find JSON array in response: {}", text);
                return Collections.emptyList();
            }
            String json = text.substring(start, end + 1);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("[AiReview] Failed to parse Claude response: {}", text, e);
            return Collections.emptyList();
        }
    }
}
