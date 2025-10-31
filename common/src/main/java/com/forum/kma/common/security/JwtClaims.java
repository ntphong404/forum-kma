package com.forum.kma.common.security;

/**
 * @param type "access" hoặc "refresh"
 */
public record JwtClaims(String userId, String roleId, String sid, String type) {
}
