package com.forum.kma.postservice.dto;

import java.util.UUID;

public record AuthorizationInput(
        String userId,
        String roleId,
        String action,
        String resourceType,
        String resourceId,
        String resourceAuthorId,
        String resourceStatus
) { }