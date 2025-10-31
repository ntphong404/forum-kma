package com.forum.kma.authservice.dto;

public record RefreshRequest(String accessToken, String refreshToken) {}
