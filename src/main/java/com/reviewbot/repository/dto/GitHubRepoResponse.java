package com.reviewbot.repository.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepoResponse(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("private") boolean isPrivate,
        @JsonProperty("description") String description,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("default_branch") String defaultBranch
) {}
