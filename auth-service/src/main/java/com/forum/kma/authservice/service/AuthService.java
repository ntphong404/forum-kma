package com.forum.kma.authservice.service;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.authservice.dto.DeviceInfo;
import com.forum.kma.authservice.dto.LoginRequest;
import com.forum.kma.authservice.dto.RefreshRequest;
import com.forum.kma.authservice.dto.RegisterRequest;
import com.forum.kma.authservice.model.Role;
import com.forum.kma.authservice.model.User;
import com.forum.kma.authservice.dto.AuthResponse;
import com.forum.kma.authservice.repository.RoleRepository;
import com.forum.kma.authservice.repository.UserRepository;
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
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    private static final String PERM_PREFIX = "PERM:";

    // === NGHIỆP VỤ CHÍNH ===

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
                                            return new AppException(AuthErrorCode.SOMETHING_WRONG);
                                        });
                            })
                            .flatMap(savedUser -> createSessionAndTokens(savedUser, exchange));
                });
    }

    public Mono<AuthResponse> login(LoginRequest req, ServerWebExchange exchange) {
        return userRepository.findByUsername(req.getUsername())
                .switchIfEmpty(Mono.error(new AppException(CommonErrorCode.USER_NOT_EXISTED)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                        return Mono.error(new AppException(CommonErrorCode.UNAUTHENTICATED));
                    }
                    return createSessionAndTokens(user, exchange);
                });
    }

    public Mono<AuthResponse> refreshToken(RefreshRequest request, ServerWebExchange exchange) {
        // 1. Xác thực RT và lấy Claims
        JwtClaims claims;
        try {
            claims = jwtUtil.validateToken(request.refreshToken());
            // SỬA ĐỔI: Dùng accessor của record
            if (!"refresh".equals(claims.type()) || claims.sid() == null) {
                return Mono.error(new AppException(AuthErrorCode.INVALID_TOKEN_TYPE));
            }
        } catch (Exception e) {
            return Mono.error(new RuntimeException(e.getMessage()));
        }

        String userId = claims.userId();
        String roleId = claims.roleId();
        String oldSessionId = claims.sid(); // Lấy Session ID từ RT

        // 2. Kiểm tra Session cũ có còn hợp lệ không (Rolling check)
        return sessionService.isSessionActive(userId, oldSessionId)
                .flatMap(isActive -> {
                    if (!isActive) {
                        log.warn("Refresh token reuse detected or session revoked: {}", oldSessionId);
                        return Mono.error(new AppException(AuthErrorCode.SESSION_REVOKED));
                    }

                    String newSessionId = UUID.randomUUID().toString();
                    DeviceInfo deviceInfo = extractDeviceInfo(exchange); // Lấy DeviceInfo mới

                    // 3. Thực hiện các tác vụ
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

    // === CÁC HÀM HELPER ===

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

        // Thêm logic phân tích User-Agent (cơ bản)
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
                .deviceType(deviceType) // Thêm loại thiết bị
                .build();
    }

    private Mono<Void> addSessionAndCachePerms(String userId, String roleId, String sessionId, DeviceInfo deviceInfo) {
        Duration accessTtl = Duration.ofMillis(jwtProperties.getAccessExpirationMs());

        // 1. Tải Permissions từ DB
        Mono<Set<String>> permissionsMono = roleRepository.findById(roleId)
                .map(Role::getPermissions)
                .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.ROLE_NOT_EXISTED)))
                .map(permissions -> permissions != null ? permissions : Collections.emptySet());

        // 2. Lưu Permissions vào Redis (Dưới dạng Set hoặc String JSON)
        // Lưu trực tiếp Set<String>, không cần serialize thủ công
        Mono<?> savePerms = permissionsMono.flatMap(perms ->
                redisTemplate.opsForValue().set(PERM_PREFIX + sessionId, perms, accessTtl)
        );

        // 3. Thêm Session ID và DeviceInfo vào Hash (SỬA ĐỔI)
        Mono<?> saveSession = sessionService.addSession(userId, sessionId, deviceInfo);

        return Mono.when(savePerms, saveSession).then();
    }

    private Mono<Void> deletePermissions(String sessionId) {
        String key = PERM_PREFIX + sessionId;
        return redisTemplate.delete(key).then();
    }
}