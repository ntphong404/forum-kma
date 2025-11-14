package com.forum.kma.authservice.dto.request;

public record RefreshRequest(String accessToken, String refreshToken) {}
