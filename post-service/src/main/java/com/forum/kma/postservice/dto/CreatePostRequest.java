package com.forum.kma.postservice.dto;

public record CreatePostRequest(
        String title,
        String content
) {
}
