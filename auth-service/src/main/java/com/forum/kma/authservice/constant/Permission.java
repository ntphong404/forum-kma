package com.forum.kma.authservice.constant;

import lombok.Getter;
import java.util.Collections;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public enum Permission {

    // --- Permissions cơ bản ---
    USER_READ("user:read"),
    USER_CREATE("user:create"),
    USER_UPDATE("user:update"),
    USER_DELETE("user:delete"),

    POST_READ("post:read"),
    POST_CREATE("post:create"),
    POST_UPDATE("post:update"),
    POST_DELETE("post:delete"),

    // --- Permissions quản trị ---
    ROLE_MANAGEMENT("role:manage"),
    CONFIG_ACCESS("config:access");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    // Phương thức tiện ích để gom nhiều Permissions thành Set<String>
    public static Set<String> getPermissionsAsStrings(Permission... permissions) {
        if (permissions == null || permissions.length == 0) {
            return Collections.emptySet();
        }
        return Arrays.stream(permissions)
                .map(Permission::getPermission)
                .collect(Collectors.toSet());
    }
}