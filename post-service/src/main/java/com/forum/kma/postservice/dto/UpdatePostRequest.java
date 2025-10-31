package com.forum.kma.postservice.dto;

public record UpdatePostRequest(
        String newTitle,
        String newContent
) {
}
