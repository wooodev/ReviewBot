package com.reviewbot.review.dto;

import com.reviewbot.review.domain.ReviewComment;

import java.util.UUID;

public record CommentResponse(
        UUID id,
        String filePath,
        Integer lineNumber,
        String comment,
        String severity,
        String category
) {
    public static CommentResponse from(ReviewComment c) {
        return new CommentResponse(
                c.getId(),
                c.getFilePath(),
                c.getLineNumber(),
                c.getComment(),
                c.getSeverity() != null ? c.getSeverity().name().toLowerCase() : null,
                c.getCategory() != null ? c.getCategory().name().toLowerCase() : null
        );
    }
}
