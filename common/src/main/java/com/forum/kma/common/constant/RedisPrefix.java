package com.forum.kma.common.constant;

import lombok.Getter;

@Getter
public enum RedisPrefix {
    OTP_EMAIL_PREFIX("otp:email:"),
    OTP_USER_PREFIX("otp:user:"),
    PERMISSION_PREFIX("perm:"),
    ;

    private final String prefix;

    RedisPrefix(String prefix) {
        this.prefix = prefix;
    }

}
