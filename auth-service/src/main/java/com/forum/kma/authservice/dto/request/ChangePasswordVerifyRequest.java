package com.forum.kma.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordVerifyRequest(
        @NotBlank String otp
) {
}
