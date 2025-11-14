package com.forum.kma.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.event.AuthEvent;
import com.forum.kma.notification.dto.EmailRequest;
import com.forum.kma.notification.dto.Recipient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    private final KafkaReceiver<String, String> authKafkaReceiver;
    private final EmailSender emailSender;
    private final ThymeleafTemplateRenderer templateRenderer;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void consume() {
        authKafkaReceiver.receive()
                .flatMap(record ->
                        Mono.fromCallable(() -> mapper.readValue(record.value(), AuthEvent.class))
                                .flatMap(event -> {
                                    // Build email based on action
                                    String subject;
                                    String html;
                                    // Render template for each action
                                    Map<String, Object> model = Map.of(
                                            "name", event.getUserName() != null ? event.getUserName() : "there",
                                            "otp", event.getOtp()
                                    );

                                        String templateName = switch (event.getAction()) {
                                            case FORGOT_PASSWORD -> "forgot-password";
                                            case VERIFY_EMAIL -> "verify-email";
                                            case TWO_FACTOR_LOGIN -> "two-factor";
                                            case CHANGE_PASSWORD ->  "change-password";
                                            // Default case covers all other AuthEvent.Action values
                                            default -> "notification";
                                        };

                                    String subjectFinal = switch (event.getAction()) {
                                        case FORGOT_PASSWORD -> "Password reset OTP";
                                        case VERIFY_EMAIL -> "Verify your email address";
                                        case TWO_FACTOR_LOGIN -> "Your two-factor authentication code";
                                        case CHANGE_PASSWORD -> "Change password OTP";
                                        // Default case covers all other AuthEvent.Action values
                                        default -> "Notification";
                                    };

                                    return templateRenderer.render(templateName, model)
                                            .flatMap(htmlRendered -> {
                                                EmailRequest email = new EmailRequest(
                                                        List.of(new Recipient(event.getUserName(), event.getEmail())),
                                                        subjectFinal,
                                                        htmlRendered
                                                );
                                                return emailSender.send(email)
                                                        .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge()));
                                            });
                                })
                                .onErrorResume(e -> {
                                    log.error("Error processing auth record: {}", e.getMessage(), e);
                                    try { record.receiverOffset().acknowledge(); } catch (Exception ignored) { }
                                    return Mono.empty();
                                })
                        , 8)
                .subscribe();
    }
}