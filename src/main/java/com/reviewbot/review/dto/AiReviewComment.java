package com.reviewbot.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Claude API가 반환하는 개별 리뷰 코멘트.
 * GitHub API 요청 및 DB 저장에 공통으로 사용된다.
 */
public record AiReviewComment(
        @JsonProperty("filePath")   String filePath,
        @JsonProperty("lineNumber") Integer lineNumber,
        @JsonProperty("severity")   String severity,
        @JsonProperty("category")   String category,
        @JsonProperty("comment")    String comment
) {}
