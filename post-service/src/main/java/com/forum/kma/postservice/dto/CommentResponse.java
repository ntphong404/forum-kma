package com.forum.kma.postservice.dto;

import com.forum.kma.postservice.model.Comment;

public record CommentResponse(
        String commentId,
        String postId,
        String authorId,
        String content,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        int reactionCount
) {
}
