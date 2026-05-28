package com.reviewbot.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class WebhookPayload {

    private String action;

    @JsonProperty("pull_request")
    private PullRequestPayload pullRequest;

    private RepositoryPayload repository;

    @Getter
    public static class PullRequestPayload {
        private int number;
        private String title;
        private UserPayload user;

        @JsonProperty("head")
        private HeadPayload head;
    }

    @Getter
    public static class UserPayload {
        private String login;
    }

    @Getter
    public static class HeadPayload {
        private String sha;
    }

    @Getter
    public static class RepositoryPayload {
        private String id;

        @JsonProperty("full_name")
        private String fullName;
    }
}
