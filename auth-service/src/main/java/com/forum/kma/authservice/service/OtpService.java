package com.forum.kma.authservice.service;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;

import static com.forum.kma.common.constant.RedisPrefix.OTP_EMAIL_PREFIX;
import static com.forum.kma.common.constant.RedisPrefix.OTP_USER_PREFIX;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    private static final int OTP_LENGTH = 6;

    /**
     * Generates and saves OTP for a user who is NOT logged in (using Email).
     * @param email User's email
     * @return Mono containing the generated OTP, or Mono.error if an OTP already exists.
     */
    public Mono<String> generateAndSaveByEmail(String email) {
        String key = OTP_EMAIL_PREFIX + email.toLowerCase();
        return generateAndSaveOtp(key);
    }

    /**
     * Generates and saves OTP for a user who IS logged in (using User ID).
     * @param userId User ID
     * @return Mono containing the generated OTP, or Mono.error if an OTP already exists.
     */
    public Mono<String> generateAndSaveByUserId(String userId) {
        String key = OTP_USER_PREFIX + userId;
        return generateAndSaveOtp(key);
    }

    /**
     * Common function to generate and save OTP, checking for existence first using setIfAbsent.
     * @param key Redis Key (e.g., otp:email:user@example.com or otp:user:123)
     * @return Mono containing the newly created OTP or an error if it already exists.
     */
    private Mono<String> generateAndSaveOtp(String key) {
        String otp = generateOtp();

        return redisTemplate.opsForValue()
                .setIfAbsent(key, otp, OTP_TTL)
                .flatMap(saved -> {
                    if (saved != null && saved) {
                        return Mono.just(otp);
                    } else {
                        return Mono.error(new AppException(AuthErrorCode.OTP_ALREADY_SENT));
                    }
                });
    }

    // --- Verification Methods ---

    /**
     * Common function to verify OTP.
     * @param keyPrefix Prefix of the OTP type (OTP_EMAIL_PREFIX or OTP_USER_PREFIX)
     * @param entityValue Email (for email prefix) or User ID (for user prefix)
     * @param otp The code to verify
     * @param consume If true, delete the OTP from Redis upon successful verification.
     */
    private Mono<Boolean> verifyOtp(String keyPrefix, String entityValue, String otp, boolean consume) {
        String key = keyPrefix + entityValue.toLowerCase();

        return redisTemplate.opsForValue().get(key)
                .map(val -> otp.equals(String.valueOf(val)))
                .defaultIfEmpty(false)
                .flatMap(match -> {
                    if (!match) return Mono.just(false);

                    if (consume) {
                        return redisTemplate.delete(key).thenReturn(true);
                    } else {
                        return Mono.just(true);
                    }
                });
    }

    /**
     * Verifies and consumes the OTP for an Email (Default behavior).
     */
    public Mono<Boolean> verifyByEmail(String email, String otp, boolean consume) {
        return verifyOtp(OTP_EMAIL_PREFIX.toString(), email, otp, consume);
    }

    /**
     * Verifies and consumes the OTP for a User ID (Default behavior).
     */
    public Mono<Boolean> verifyByUserId(String userId, String otp, boolean consume) {
        return verifyOtp(OTP_USER_PREFIX.toString(), userId, otp, consume);
    }

    // --- OTP Generation Function ---

    private String generateOtp() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
    }
}