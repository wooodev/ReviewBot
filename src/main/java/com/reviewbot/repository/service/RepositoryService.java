package com.reviewbot.repository.service;

import com.reviewbot.auth.domain.User;
import com.reviewbot.auth.repository.UserRepository;
import com.reviewbot.common.exception.ResourceNotFoundException;
import com.reviewbot.github.service.GitHubService;
import com.reviewbot.repository.domain.GithubRepository;
import com.reviewbot.repository.domain.RepositoryConfig;
import com.reviewbot.repository.dto.*;
import com.reviewbot.repository.repository.GithubRepositoryRepository;
import com.reviewbot.repository.repository.RepositoryConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final GithubRepositoryRepository githubRepositoryRepository;
    private final RepositoryConfigRepository repositoryConfigRepository;
    private final UserRepository userRepository;
    private final GitHubService gitHubService;

    public List<GitHubRepoResponse> getGithubRepositories(String userId) {
        String accessToken = getAccessToken(userId);
        return gitHubService.getUserRepositories(accessToken);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> getLinkedRepositories(String userId) {
        return githubRepositoryRepository
                .findAllByUserIdAndActiveTrue(UUID.fromString(userId))
                .stream()
                .map(RepositoryResponse::from)
                .toList();
    }

    @Transactional
    public RepositoryResponse registerRepository(String userId, RegisterRepositoryRequest request) {
        UUID uid = UUID.fromString(userId);

        if (githubRepositoryRepository.existsByUserIdAndGithubRepoId(uid, request.githubRepoId())) {
            throw new IllegalArgumentException("Repository already registered: " + request.fullName());
        }

        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String webhookId = gitHubService.createWebhook(user.getAccessToken(), request.fullName());

        GithubRepository repo = GithubRepository.of(user, request.githubRepoId(),
                                                     request.fullName(), webhookId);
        githubRepositoryRepository.save(repo);
        repositoryConfigRepository.save(RepositoryConfig.defaultConfig(repo));

        return RepositoryResponse.from(repo);
    }

    @Transactional
    public void deregisterRepository(String userId, UUID repoId) {
        GithubRepository repo = githubRepositoryRepository
                .findByIdAndUserId(repoId, UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));

        if (repo.getWebhookId() != null) {
            String accessToken = getAccessToken(userId);
            gitHubService.deleteWebhook(accessToken, repo.getFullName(), repo.getWebhookId());
        }

        githubRepositoryRepository.delete(repo);
    }

    @Transactional(readOnly = true)
    public RepositoryConfigResponse getConfig(String userId, UUID repoId) {
        validateOwnership(userId, repoId);
        RepositoryConfig config = repositoryConfigRepository.findByRepositoryId(repoId)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found for repository"));
        return RepositoryConfigResponse.from(config);
    }

    @Transactional
    public RepositoryConfigResponse updateConfig(String userId, UUID repoId,
                                                 RepositoryConfigRequest request) {
        validateOwnership(userId, repoId);
        RepositoryConfig config = repositoryConfigRepository.findByRepositoryId(repoId)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found for repository"));

        config.update(
                request.reviewLanguage(),
                request.reviewDepth(),
                request.focusAreas().toArray(String[]::new),
                request.ignorePatterns().toArray(String[]::new)
        );
        repositoryConfigRepository.save(config);
        return RepositoryConfigResponse.from(config);
    }

    private void validateOwnership(String userId, UUID repoId) {
        if (!githubRepositoryRepository.findByIdAndUserId(repoId, UUID.fromString(userId)).isPresent()) {
            throw new ResourceNotFoundException("Repository not found");
        }
    }

    private String getAccessToken(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(User::getAccessToken)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
