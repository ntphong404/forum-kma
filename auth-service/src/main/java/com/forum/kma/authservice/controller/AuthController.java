package com.forum.kma.authservice.controller;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.authservice.dto.request.*;
import com.forum.kma.authservice.dto.response.AuthResponse;
import com.forum.kma.authservice.service.AuthService;
import com.forum.kma.authservice.service.OtpService;
import com.forum.kma.authservice.service.AuthEventProducer;
import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.common.event.AuthEvent;
import com.forum.kma.common.exception.AppException;
import com.forum.kma.common.exception.CommonErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
// security context is handled inside AuthService for the endpoints that need it

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final AuthEventProducer authEventProducer;

    @PostMapping("/register")
    public Mono<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request, ServerWebExchange exchange) {

    return authService.register(request, exchange)
            .map(authResponse -> ApiResponse.success("Register success", authResponse));
    }

    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, ServerWebExchange exchange) {

            return authService.login(request, exchange)
                            .map(authResponse -> ApiResponse.success("Login success", authResponse));
    }

    @PostMapping("/login/verify")
    public Mono<ApiResponse<AuthResponse>> completeLoginAfter2fa(@Valid @RequestBody VerifyOtpRequest req, ServerWebExchange exchange) {
            return authService.completeLoginAfter2fa(req.getEmail(), req.getOtp(), exchange)
                    .map(authResponse -> ApiResponse.success("Login success", authResponse))
                    .onErrorResume(AppException.class, ex -> Mono.just(ApiResponse.error(ex.getErrorCode().getCode(), ex.getMessage())));
    }

    @PostMapping("/refresh")
    public Mono<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshRequest request, ServerWebExchange exchange) {

    return authService.refreshToken(request, exchange)
            .map(authResponse -> ApiResponse.success("Refreshed token", authResponse));
    }

    @PostMapping("/verify-email")
    public Mono<ApiResponse<String>> sendVerifyEmail() {
        return authService.verifyEmail()
                .map(msg -> ApiResponse.success("OTP sent", msg))
                .onErrorResume(AppException.class,
                        e -> Mono.just(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage())));
    }

    @PostMapping("/verify-email/complete")
    public Mono<ApiResponse<String>> completeVerifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        return authService.completeVerifyEmail(req.otp())
                .map(valid -> ApiResponse.success("OTP valid", "ok"))
                .onErrorResume(AppException.class,
                        e -> Mono.just(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage())));
    }

    @PostMapping("/forgot-password")
    public Mono<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
            String email = req.getEmail();
            return authService.forgotPassword(email)
                    .map(resp -> ApiResponse.success("OTP sent", resp))
                    .onErrorResume(AppException.class,
                            e -> Mono.just(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage())));
    }

    @PostMapping("/verify-otp")
    public Mono<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
                    // Verify OTP without consuming it (allow user to enter OTP and only consume when resetting password)
                    return otpService.verifyByEmail(req.getEmail(), req.getOtp(), false)
                                    .flatMap(valid -> {
                                            if (valid) return Mono.just(ApiResponse.success("OTP valid", "ok"));
                                            return Mono.just(ApiResponse.error(400, "Invalid or expired OTP"));
                                    });
    }

    @PostMapping("/reset-password")
    public Mono<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
            // For reset password we want to consume (delete) the OTP when verification succeeds
        return otpService.verifyByEmail(req.getEmail(), req.getOtp(), true)
                    .flatMap(valid -> {
                        if (!valid) return Mono.just(ApiResponse.error(400, "Invalid or expired OTP"));
                        // find user by email and update password
                        return authService.resetPasswordByEmail(req.getEmail(), req.getNewPassword())
                                .map(u -> ApiResponse.success("Password updated", "ok"));
                    });
    }

    @PostMapping("/change-password")
    public Mono<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
            return authService.initiateChangePassword(req.oldPassword(), req.newPassword())
                    .map(resp -> ApiResponse.success(resp, "ok"))
                    .onErrorResume(AppException.class, e -> Mono.just(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage())));
    }

    @PostMapping("/change-password/verify")
    public Mono<ApiResponse<String>> changePasswordVerify(@Valid @RequestBody ChangePasswordVerifyRequest req) {
            return authService.confirmChangePassword(req.otp())
                    .map(resp -> ApiResponse.success(resp, "ok"))
                    .onErrorResume(AppException.class, e -> Mono.just(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage())));
    }
}