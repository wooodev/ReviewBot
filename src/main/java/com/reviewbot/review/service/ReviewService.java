package com.reviewbot.review.service;

import com.reviewbot.common.exception.ResourceNotFoundException;
import com.reviewbot.common.ratelimit.RateLimitService;
import com.reviewbot.github.dto.WebhookPayload;
import com.reviewbot.github.service.GitHubService;
import com.reviewbot.repository.domain.GithubRepository;
import com.reviewbot.repository.domain.RepositoryConfig;
import com.reviewbot.repository.repository.GithubRepositoryRepository;
import com.reviewbot.repository.repository.RepositoryConfigRepository;
import com.reviewbot.review.domain.Review;
import com.reviewbot.review.domain.ReviewComment;
import com.reviewbot.review.domain.ReviewStatus;
import com.reviewbot.review.domain.Severity;
import com.reviewbot.review.dto.*;
import com.reviewbot.review.repository.ReviewCommentRepository;
import com.reviewbot.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final RepositoryConfigRepository repositoryConfigRepository;
    private final GitHubService gitHubService;
    private final AiReviewService aiReviewService;
    private final RateLimitService rateLimitService;

    // ── Webhook 자동 트리거 ────────────────────────────────────────────────────

    @Async("reviewTaskExecutor")
    public void triggerReviewAsync(WebhookPayload payload) {
        String githubRepoId = payload.getRepository().getId();
        String repoFullName = payload.getRepository().getFullName();
        int prNumber        = payload.getPullRequest().getNumber();
        String prTitle      = payload.getPullRequest().getTitle();
        String prAuthor     = payload.getPullRequest().getUser().getLogin();
        String headSha      = payload.getPullRequest().getHead().getSha();

        // 1. 저장소 조회 (User 즉시 로딩)
        GithubRepository repo = githubRepositoryRepository
                .findByGithubRepoIdAndActiveTrue(githubRepoId)
                .orElse(null);
        if (repo == null) {
            log.warn("[Review] No active repo for github_repo_id={}", githubRepoId);
            return;
        }

        String userId      = repo.getUser().getId().toString();
        String accessToken = repo.getUser().getAccessToken();
        String plan        = repo.getUser().getPlan().name().toLowerCase();
        UUID repoId        = repo.getId();

        // 2. Review 레코드 생성 (PROCESSING)
        GithubRepository repoRef = githubRepositoryRepository.getReferenceById(repoId);
        Review review = reviewRepository.save(Review.of(repoRef, prNumber, prTitle, prAuthor));

        try {
            // 3. Rate limit 체크
            rateLimitService.checkAndConsume(userId, plan);

            // 4. GitHub에서 PR diff 조회
            String diff = gitHubService.getPullRequestDiff(accessToken, repoFullName, prNumber);

            // 5. 저장소 설정 로드
            RepositoryConfig config = repositoryConfigRepository
                    .findByRepositoryId(repoId)
                    .orElse(null);

            // 6. Claude API 코드 분석
            List<AiReviewComment> aiComments = aiReviewService.analyzeCode(diff, config);
            log.info("[Review] PR #{} analyzed — {} comments", prNumber, aiComments.size());

            // 7. GitHub PR에 리뷰 댓글 등록
            String githubReviewId = gitHubService.postReviewComments(
                    accessToken, repoFullName, prNumber, headSha, aiComments);

            // 8. DB 저장
            List<ReviewComment> comments = aiComments.stream()
                    .map(ai -> ReviewComment.of(review, ai))
                    .toList();
            reviewCommentRepository.saveAll(comments);

            // 9. 상태 업데이트 (COMPLETED)
            review.complete(githubReviewId);
            reviewRepository.save(review);
            log.info("[Review] PR #{} in {} — completed (reviewId={})", prNumber, repoFullName, review.getId());

        } catch (Exception e) {
            log.error("[Review] PR #{} in {} — failed: {}", prNumber, repoFullName, e.getMessage(), e);
            review.fail(e.getMessage());
            reviewRepository.save(review);
        }
    }

    // ── 수동 트리거 ───────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponse triggerManual(String userId, ManualReviewRequest request) {
        UUID uid = UUID.fromString(userId);

        // @EntityGraph 메서드로 User 즉시 로딩 — @Async 경계 전에 프리미티브 추출
        GithubRepository repo = githubRepositoryRepository
                .findWithUserByIdAndUserId(request.repositoryId(), uid)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));

        Review review = reviewRepository.save(
                Review.of(githubRepositoryRepository.getReferenceById(repo.getId()),
                        request.prNumber(), null, null));

        // 엔티티 대신 프리미티브 전달 → LazyInitializationException 방지
        triggerManualAsync(
                review.getId(),
                repo.getId(),
                repo.getFullName(),
                repo.getUser().getAccessToken(),
                repo.getUser().getId().toString(),
                repo.getUser().getPlan().name().toLowerCase(),
                request.prNumber()
        );

        return ReviewResponse.from(review);
    }

    @Async("reviewTaskExecutor")
    public void triggerManualAsync(UUID reviewId, UUID repoId, String repoFullName,
                                   String accessToken, String ownerId, String plan, int prNumber) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) return;

        try {
            rateLimitService.checkAndConsume(ownerId, plan);

            String diff = gitHubService.getPullRequestDiff(accessToken, repoFullName, prNumber);
            RepositoryConfig config = repositoryConfigRepository.findByRepositoryId(repoId).orElse(null);
            List<AiReviewComment> aiComments = aiReviewService.analyzeCode(diff, config);

            String githubReviewId = gitHubService.postReviewComments(
                    accessToken, repoFullName, prNumber, null, aiComments);

            reviewCommentRepository.saveAll(
                    aiComments.stream().map(ai -> ReviewComment.of(review, ai)).toList());

            review.complete(githubReviewId);
            reviewRepository.save(review);

        } catch (Exception e) {
            log.error("[Review] Manual review failed for PR #{}: {}", prNumber, e.getMessage(), e);
            review.fail(e.getMessage());
            reviewRepository.save(review);
        }
    }

    // ── 대시보드 조회 ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(String userId, Pageable pageable) {
        return reviewRepository
                .findAllByRepositoryUserIdOrderByCreatedAtDesc(UUID.fromString(userId), pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByRepository(String userId, UUID repoId, Pageable pageable) {
        return reviewRepository
                .findAllByRepositoryIdAndRepositoryUserIdOrderByCreatedAtDesc(
                        repoId, UUID.fromString(userId), pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public ReviewDetailResponse getReviewDetail(String userId, UUID reviewId) {
        Review review = reviewRepository.findByIdAndRepositoryUserId(reviewId, UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        List<CommentResponse> comments = reviewCommentRepository.findAllByReviewId(reviewId)
                .stream().map(CommentResponse::from).toList();

        return ReviewDetailResponse.from(review, comments);
    }

    @Transactional(readOnly = true)
    public ReviewStatsResponse getStats(String userId) {
        UUID uid = UUID.fromString(userId);
        return new ReviewStatsResponse(
                reviewRepository.countByUserId(uid),
                reviewRepository.countByUserIdAndStatus(uid, ReviewStatus.COMPLETED),
                reviewRepository.countByUserIdAndStatus(uid, ReviewStatus.FAILED),
                reviewCommentRepository.countByUserId(uid),
                reviewCommentRepository.countByUserIdAndSeverity(uid, Severity.CRITICAL)
        );
    }
}
