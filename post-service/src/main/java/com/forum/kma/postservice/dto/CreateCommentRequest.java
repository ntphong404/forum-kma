package com.forum.kma.postservice.dto;

public record CreateCommentRequest(
        String postId,
        String content
) {
}
