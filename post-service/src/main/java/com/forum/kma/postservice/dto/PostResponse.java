package com.forum.kma.postservice.dto;

import com.forum.kma.postservice.model.Post;

public record PostResponse(
        String postId,
        String title,
        String content,
        String authorId,
        Post.Status status,
        int reactionCount
) {
}
