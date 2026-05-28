package com.reviewbot.repository.controller;

import com.reviewbot.common.response.ApiResponse;
import com.reviewbot.repository.dto.*;
import com.reviewbot.repository.service.RepositoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    /** 내 GitHub 저장소 목록 (GitHub API 프록시) */
    @GetMapping("/api/v1/github/repos")
    public ResponseEntity<ApiResponse<List<GitHubRepoResponse>>> listGithubRepos(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(repositoryService.getGithubRepositories(userId)));
    }

    /** 연동된 저장소 목록 */
    @GetMapping("/api/v1/repositories")
    public ResponseEntity<ApiResponse<List<RepositoryResponse>>> listLinked(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(repositoryService.getLinkedRepositories(userId)));
    }

    /** 저장소 연동 */
    @PostMapping("/api/v1/repositories")
    public ResponseEntity<ApiResponse<RepositoryResponse>> register(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody RegisterRepositoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(repositoryService.registerRepository(userId, request)));
    }

    /** 저장소 연동 해제 */
    @DeleteMapping("/api/v1/repositories/{id}")
    public ResponseEntity<ApiResponse<Void>> deregister(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {
        repositoryService.deregisterRepository(userId, id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /** 리뷰 설정 조회 */
    @GetMapping("/api/v1/repositories/{id}/config")
    public ResponseEntity<ApiResponse<RepositoryConfigResponse>> getConfig(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(repositoryService.getConfig(userId, id)));
    }

    /** 리뷰 설정 수정 */
    @PutMapping("/api/v1/repositories/{id}/config")
    public ResponseEntity<ApiResponse<RepositoryConfigResponse>> updateConfig(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id,
            @Valid @RequestBody RepositoryConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(repositoryService.updateConfig(userId, id, request)));
    }
}
