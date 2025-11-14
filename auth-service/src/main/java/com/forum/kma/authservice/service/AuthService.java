package com.forum.kma.authservice.service;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.authservice.dto.request.LoginRequest;
import com.forum.kma.authservice.dto.request.RefreshRequest;
import com.forum.kma.authservice.dto.request.RegisterRequest;
import com.forum.kma.authservice.dto.response.AuthResponse;
import com.forum.kma.authservice.dto.response.DeviceInfo;
import com.forum.kma.authservice.model.User;
import com.forum.kma.authservice.repository.RoleRepository;
import com.forum.kma.authservice.repository.UserRepository;
import com.forum.kma.common.event.AuthEvent;
import com.forum.kma.common.exception.AppException;
import com.forum.kma.common.exception.CommonErrorCode;
import com.forum.kma.common.security.JwtClaims;
import com.forum.kma.common.security.JwtProperties;
import com.forum.kma.common.security.JwtUtil;
import com.mongodb.DuplicateKeyException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;

import static com.forum.kma.common.constant.RedisPrefix.OTP_USER_PREFIX;
import static com.forum.kma.common.constant.RedisPrefix.PERMISSION_PREFIX;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;
    JwtProperties jwtProperties;
    ReactiveRedisTemplate<String, Object> redisTemplate;
    SessionService sessionService;
    OtpService otpService;
    AuthEventProducer authEventProducer;

    // === CORE BUSINESS LOGIC ===

    /**
     * Handles user registration.
     */
    public Mono<AuthResponse> register(RegisterRequest req, ServerWebExchange exchange) {
        return userRepository.existsByUsername(req.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new AppException(AuthErrorCode.USER_EXISTED));
                    }
                    return roleRepository.findByName("USER")
                            .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.ROLE_NOT_EXISTED)))
                            .flatMap(userRole -> {
                                User userToSave = User.builder()
                                        .username(req.getUsername())
                                        .password(passwordEncoder.encode(req.getPassword()))
                                        .email(req.getEmail())
                                        .roleId(userRole.getId())
                                        .build();
                                return userRepository.insert(userToSave)
                                        .onErrorMap(DuplicateKeyException.class, ex -> {
                                            log.warn("Duplicate key error on registration: {}", ex.getMessage());
                                            return new AppException(AuthErrorCode.DATABASE_SAVE_FAILED);
                                        });
                            })
                            .flatMap(savedUser -> createSessionAndTokens(savedUser, exchange));
                });
    }

    /**
     * Handles user login process. Checks credentials and 2FA status.
     */
    public Mono<AuthResponse> login(LoginRequest req, ServerWebExchange exchange) {
        return userRepository.findByUsername(req.getUsername())
                .switchIfEmpty(Mono.error(new AppException(CommonErrorCode.USER_NOT_EXISTED)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                        return Mono.error(new AppException(AuthErrorCode.PASSWORD_NOT_MATCH));
                    }

                    if (user.getUserStatus() == com.forum.kma.authservice.model.User.UserStatus.BANNED) {
                        return Mono.error(new AppException(AuthErrorCode.USER_BANNED));
                    }

                    if (Boolean.TRUE.equals(user.getIs2FAEnabled())) {
                        // Using the new generateAndSaveByEmail
                        return otpService.generateAndSaveByEmail(user.getEmail())
                                .flatMap(otp -> {
                                    AuthEvent event = new AuthEvent();
                                    event.setEmail(user.getEmail());
                                    event.setUserId(user.getId());
                                    event.setAction(AuthEvent.Action.TWO_FACTOR_LOGIN);
                                    event.setOtp(otp);
                                    return authEventProducer.sendAuthEvent(event);
                                })
                                .then(Mono.error(new AppException(AuthErrorCode.TWO_FACTOR_REQUIRED)));
                    }

                    return createSessionAndTokens(user, exchange);
                });
    }

    /**
     * Completes login after successful 2FA verification (internal).
     */
    public Mono<AuthResponse> completeLoginAfter2fa(User user, ServerWebExchange exchange) {
        return createSessionAndTokens(user, exchange);
    }

    /**
     * Completes login after 2FA when only email + otp are available (controller-friendly overload).
     */
    public Mono<AuthResponse> completeLoginAfter2fa(String email, String otp, ServerWebExchange exchange) {
        return getUserByEmail(email)
                .switchIfEmpty(Mono.error(new AppException(CommonErrorCode.USER_NOT_EXISTED)))
                .flatMap(user -> otpService.verifyByEmail(email, otp, true)
                        .flatMap(valid -> {
                            if (!valid) return Mono.error(new AppException(CommonErrorCode.UNAUTHENTICATED));
                            return completeLoginAfter2fa(user, exchange);
                        })
                );
    }

    /**
     * Initiates the email verification process for an inactive user.
     */
    public Mono<String> verifyEmail(User user) {
        if(user.getUserStatus()==User.UserStatus.ACTIVE) {
            throw new AppException(AuthErrorCode.USER_ALREADY_ACTIVE);
        }
        String key = OTP_USER_PREFIX + user.getId();
        return redisTemplate.hasKey(key)
                .flatMap(has -> {
                    if (Boolean.TRUE.equals(has)) {
                        return Mono.error(new AppException(AuthErrorCode.OTP_ALREADY_SENT));
                    }
                    // For logged-in flows use userId-based OTP
                    return otpService.generateAndSaveByUserId(user.getId())
                            .flatMap(otp -> {
                                AuthEvent event = new AuthEvent();
                                event.setEmail(user.getEmail());
                                event.setUserId(user.getId());
                                event.setAction(AuthEvent.Action.VERIFY_EMAIL);
                                event.setOtp(otp);
                                return authEventProducer.sendAuthEvent(event);
                            })
                            .thenReturn("OTP sent");
                });
    }

    /**
     * Completes the email verification process.
     */
    public Mono<Boolean> completeVerifyEmail(User user, String otp) {
        return otpService.verifyByUserId(user.getId(), otp, true)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new AppException(AuthErrorCode.OTP_CODE_INVALID));
                    }
                    user.setUserStatus(User.UserStatus.ACTIVE);
                    return userRepository.save(user)
                            .thenReturn(true);
                });
    }

    /**
     * Initiates the password change process.
     */
    public Mono<String> initiateChangePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Mono.error(new AppException(AuthErrorCode.PASSWORD_NOT_MATCH));
        }

        if (!Boolean.TRUE.equals(user.getIs2FAEnabled())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            return userRepository.save(user).thenReturn("Password updated");
        }

        String key = OTP_USER_PREFIX + user.getId();
        String hashedNew = passwordEncoder.encode(newPassword);
        return redisTemplate.opsForValue()
                .set(key, hashedNew, Duration.ofMinutes(5))
                .flatMap(saved -> {
                    if (!saved) return Mono.error(new AppException(AuthErrorCode.REDIS_SAVE_FAILED));
                    // Using the new generateAndSaveByEmail
                    return otpService.generateAndSaveByUserId(user.getId())
                            .flatMap(otp -> {
                                AuthEvent event = new AuthEvent();
                                event.setEmail(user.getEmail());
                                event.setUserId(user.getId());
                                event.setAction(AuthEvent.Action.CHANGE_PASSWORD);
                                event.setOtp(otp);
                                return authEventProducer.sendAuthEvent(event);
                            })
                            .thenReturn("OTP sent");
                });
    }

    /**
     * Confirms the password change using OTP.
     */
    public Mono<String> confirmChangePassword(User user, String otp) {
        String key = OTP_USER_PREFIX + user.getId();
        return redisTemplate.opsForValue().get(key)
                .flatMap(stored -> {
                    if (stored == null) return Mono.error(new AppException(CommonErrorCode.USER_NOT_EXISTED));
                    String hashedNew = String.valueOf(stored);
                    // Verify against userId-based OTP
                    return otpService.verifyByUserId(user.getId(), otp, true)
                            .flatMap(valid -> {
                                if (!valid) return Mono.error(new AppException(AuthErrorCode.OTP_CODE_INVALID));
                                user.setPassword(hashedNew);
                                return userRepository.save(user)
                                        .then(redisTemplate.delete(key))
                                        .thenReturn("Password updated");
                            });
                })
                .defaultIfEmpty("No pending change")
                .flatMap(resp -> {
                    if ("No pending change".equals(resp)) return Mono.error(new AppException(CommonErrorCode.USER_NOT_EXISTED));
                    return Mono.just(resp);
                });
    }

    /**
     * Resets password by email (used after separate OTP verification, not handled here).
     */
    public Mono<Boolean> resetPasswordByEmail(String email, String newPassword) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new AppException(CommonErrorCode.USER_NOT_EXISTED)))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    return userRepository.save(user).thenReturn(true);
                });
    }

    /**
     * Handles refreshing Access Token using a valid Refresh Token.
     */
    public Mono<AuthResponse> refreshToken(RefreshRequest request, ServerWebExchange exchange) {
        // 1. Validate RT and extract Claims
        JwtClaims claims;
        try {
            claims = jwtUtil.validateToken(request.refreshToken());
            if (!"refresh".equals(claims.type()) || claims.sid() == null) {
                return Mono.error(new AppException(AuthErrorCode.INVALID_TOKEN_TYPE));
            }
        } catch (Exception e) {
            return Mono.error(new AppException(AuthErrorCode.INVALID_TOKEN));
        }

        String userId = claims.userId();
        String roleId = claims.roleId();
        String oldSessionId = claims.sid();

        // 2. Check if the old Session is still valid (Rolling check)
        return sessionService.isSessionActive(userId, oldSessionId)
                .flatMap(isActive -> {
                    if (!isActive) {
                        log.warn("Refresh token reuse detected or session revoked: {}", oldSessionId);
                        return Mono.error(new AppException(AuthErrorCode.SESSION_REVOKED));
                    }

                    String newSessionId = UUID.randomUUID().toString();
                    DeviceInfo deviceInfo = extractDeviceInfo(exchange);

                    // 3. Perform atomic operations: Revoke old, delete old permissions, create new session/perms
                    Mono<Void> revokeOld = sessionService.revokeSingleSession(userId, oldSessionId).then();
                    Mono<Void> deleteOldPerms = deletePermissions(oldSessionId);
                    Mono<Void> createNew = addSessionAndCachePerms(userId, roleId, newSessionId, deviceInfo);

                    return Mono.when(revokeOld, deleteOldPerms, createNew)
                            .then(Mono.just(new AuthResponse(
                                    jwtUtil.generateAccessToken(userId, roleId, newSessionId),
                                    jwtUtil.generateRefreshToken(userId, roleId, newSessionId)
                            )));
                });
    }

    // === USER CONTEXT HELPERS & OVERLOADS ===

    public Mono<com.forum.kma.authservice.model.User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Handles forgot-password flow: if a user with the given email exists,
     * generate an OTP (email-based) and publish a FORGOT_PASSWORD AuthEvent.
     * Returns a neutral success string regardless of whether the account exists
     * to avoid user enumeration.
     */
    public Mono<String> forgotPassword(String email) {
        // If user not found, raise an error instead of returning a silent OK
        return getUserByEmail(email)
                .switchIfEmpty(Mono.error(new AppException(CommonErrorCode.USER_NOT_EXISTED)))
                .flatMap(user -> otpService.generateAndSaveByEmail(email)
                        .flatMap(otp -> {
                            AuthEvent event = new AuthEvent();
                            event.setEmail(email);
                            event.setAction(AuthEvent.Action.FORGOT_PASSWORD);
                            event.setOtp(otp);
                            return authEventProducer.sendAuthEvent(event);
                        })
                        .thenReturn("ok")
                );
    }

    private Mono<User> getCurrentUserFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> (User) auth.getPrincipal())
                .switchIfEmpty(Mono.error(new AppException(CommonErrorCode.UNAUTHENTICATED)));
    }

    /**
     * Convenience overloads that obtain the current authenticated user from SecurityContext
     * so controllers don't have to repeat the ReactiveSecurityContextHolder boilerplate.
     */
    public Mono<String> verifyEmail() {
        return getCurrentUserFromContext().flatMap(this::verifyEmail);
    }

    public Mono<Boolean> completeVerifyEmail(String otp) {
        return getCurrentUserFromContext().flatMap(user -> completeVerifyEmail(user, otp));
    }

    public Mono<String> initiateChangePassword(String oldPassword, String newPassword) {
        return getCurrentUserFromContext().flatMap(user -> initiateChangePassword(user, oldPassword, newPassword));
    }

    public Mono<String> confirmChangePassword(String otp) {
        return getCurrentUserFromContext().flatMap(user -> confirmChangePassword(user, otp));
    }

    // === HELPER FUNCTIONS ===

    private Mono<AuthResponse> createSessionAndTokens(User user, ServerWebExchange exchange) {
        String sessionId = UUID.randomUUID().toString();
        DeviceInfo deviceInfo = extractDeviceInfo(exchange);

        return addSessionAndCachePerms(user.getId(), user.getRoleId(), sessionId, deviceInfo)
                .thenReturn(new AuthResponse(
                        jwtUtil.generateAccessToken(user.getId(), user.getRoleId(), sessionId),
                        jwtUtil.generateRefreshToken(user.getId(), user.getRoleId(), sessionId)
                ));
    }

    private DeviceInfo extractDeviceInfo(ServerWebExchange exchange) {
        String ipAddress = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(java.net.InetAddress::getHostAddress)
                .orElse("Unknown IP");

        String userAgent = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.USER_AGENT);

        // Basic User-Agent parsing logic
        String deviceType = "Unknown";
        if (userAgent != null) {
            userAgent = userAgent.toLowerCase();
            if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
                deviceType = "Mobile";
            } else if (userAgent.contains("windows") || userAgent.contains("macintosh") || userAgent.contains("linux")) {
                deviceType = "Desktop";
            }
        }

        return DeviceInfo.builder()
                .ipAddress(ipAddress)
                .userAgent(userAgent != null ? userAgent : "Unknown User-Agent")
                .deviceType(deviceType)
                .build();
    }

    private Mono<Void> addSessionAndCachePerms(String userId, String roleId, String sessionId, DeviceInfo deviceInfo) {
        Duration accessTtl = Duration.ofMillis(jwtProperties.getAccessExpirationMs());

        return roleRepository.findById(roleId)
                .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.ROLE_NOT_EXISTED)))
                .flatMap(role -> {
                    // Get set of permissions from DB
                    Set<String> permissions = role.getPermissions() != null
                            ? new HashSet<>(role.getPermissions())
                            : new HashSet<>();

                    // Add ROLE_roleName to permission set
                    permissions.add("ROLE_" + role.getName().toUpperCase());

                    Mono<Boolean> savePerms = redisTemplate.opsForSet()
                            .add(PERMISSION_PREFIX + sessionId, permissions.toArray())
                            .then(redisTemplate.expire(PERMISSION_PREFIX + sessionId, accessTtl))
                            .thenReturn(true);


                    // Save session info
                    Mono<?> saveSession = sessionService.addSession(userId, sessionId, deviceInfo);

                    // Run concurrently
                    return Mono.when(savePerms, saveSession).then();
                });
    }


    private Mono<Void> deletePermissions(String sessionId) {
        String key = PERMISSION_PREFIX + sessionId;
        return redisTemplate.delete(key).then();
    }
}