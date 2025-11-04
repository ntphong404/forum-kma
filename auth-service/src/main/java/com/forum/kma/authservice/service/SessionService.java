package com.forum.kma.authservice.service;

import com.forum.kma.authservice.dto.DeviceInfo;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

// Service chuyên quản lý Redis Hash cho sessions
@Service
public class SessionService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    // Prefix cho Key (HASH)
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";

    // TTL cho HASH (nên bằng TTL của Refresh Token)
    private static final Duration SESSION_TTL = Duration.ofDays(1);

    public SessionService(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    private String createKey(String userId) {
        return USER_SESSIONS_PREFIX + userId;
    }

    /**
     * 1. Thêm/Cập nhật một phiên vào HASH
     */
    public Mono<Void> addSession(String userId, String sessionId, DeviceInfo deviceInfo) {
        String key = createKey(userId);

        // HSET: Đặt field (sessionId) và value (deviceInfo)
        return reactiveRedisTemplate.opsForHash()
                .put(key, sessionId, deviceInfo)
                .then(reactiveRedisTemplate.expire(key, SESSION_TTL)) // Luôn làm mới TTL
                .then();
    }

    /**
     * 2. Kiểm tra phiên (sessionId) có tồn tại trong HASH không
     */
    public Mono<Boolean> isSessionActive(String userId, String sessionId) {
        String key = createKey(userId);
        // HEXISTS: Kiểm tra 'field' (sessionId)
        return reactiveRedisTemplate.opsForHash().hasKey(key, sessionId);
    }

    /**
     * 3. Thu hồi một phiên (Xóa 1 field khỏi HASH)
     */
    public Mono<Boolean> revokeSingleSession(String userId, String sessionId) {
        String key = createKey(userId);
        // HDEL: Xóa 'field' (sessionId)
        return reactiveRedisTemplate.opsForHash()
                .remove(key, sessionId)
                .map(count -> count > 0);
    }

    /**
     * 4. Thu hồi TẤT CẢ phiên (Xóa HASH)
     */
    public Mono<Boolean> revokeAllSessions(String userId) {
        String key = createKey(userId);
        // DEL: Xóa toàn bộ Key (Hash)
        return reactiveRedisTemplate.delete(key)
                .map(count -> count > 0);
    }

    /**
     * 5. Lấy TẤT CẢ phiên (để hiển thị cho người dùng)
     */
    public Flux<Map.Entry<Object, Object>> getAllSessions(String userId) {
        String key = createKey(userId);
        // HGETALL: Lấy tất cả field-value
        return reactiveRedisTemplate.opsForHash().entries(key);
    }
}