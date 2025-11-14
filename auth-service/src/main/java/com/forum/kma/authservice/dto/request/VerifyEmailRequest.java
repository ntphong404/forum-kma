package com.forum.kma.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = "OTP must not be blank")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 characters")
        String otp
) {
}
