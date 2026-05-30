package com.reviewbot.review.controller;

import com.reviewbot.common.response.ApiResponse;
import com.reviewbot.review.dto.*;
import com.reviewbot.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 전체 리뷰 이력 (페이징) */
    @GetMapping("/api/v1/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> listReviews(
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviews(userId, pageable)));
    }

    /** 저장소별 리뷰 이력 */
    @GetMapping("/api/v1/repositories/{repoId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> listByRepo(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID repoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviewsByRepository(userId, repoId, pageable)));
    }

    /** 리뷰 상세 조회 */
    @GetMapping("/api/v1/reviews/{id}")
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> getDetail(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviewDetail(userId, id)));
    }

    /** 수동 리뷰 트리거 */
    @PostMapping("/api/v1/reviews/trigger")
    public ResponseEntity<ApiResponse<ReviewResponse>> trigger(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ManualReviewRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(reviewService.triggerManual(userId, request)));
    }

    /** 리뷰 통계 */
    @GetMapping("/api/v1/stats")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> stats(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getStats(userId)));
    }
}
