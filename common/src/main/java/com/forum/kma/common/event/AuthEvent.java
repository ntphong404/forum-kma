package com.forum.kma.common.event;

import lombok.Data;

@Data
public class AuthEvent {
    private String userId;
    private String userName;
    private String email;
    private Action action;
    private String otp;

    public enum Action {
        FORGOT_PASSWORD,
        VERIFY_EMAIL,
        TWO_FACTOR_LOGIN,
        CHANGE_PASSWORD,
    }
}