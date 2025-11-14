package com.forum.kma.notification.service;

import com.forum.kma.notification.config.EmailProperties;
import com.forum.kma.notification.dto.EmailRequest;
import com.forum.kma.notification.dto.Recipient;
import com.forum.kma.notification.dto.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class BrevoEmailSender implements EmailSender {

    private final WebClient webClient;
    private final EmailProperties props;

    public BrevoEmailSender(@Qualifier("brevoWebClient") WebClient webClient,
                            EmailProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    public Mono<Void> send(EmailRequest req) {
        String apiKey = props.getBrevo().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Brevo API key is not configured. Set environment variable BREVO_API_KEY or configure email.brevo.apiKey");
            return Mono.error(new IllegalStateException("Brevo API key not configured"));
        }

        Sender sender = new Sender(
                props.getBrevo().getSender().getName(),
                props.getBrevo().getSender().getEmail()
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", req.to());
        payload.put("htmlContent", req.htmlContent());
        payload.put("subject", req.subject());

        return webClient.post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("empty-body")
                        .flatMap(body -> Mono.error(new RuntimeException(
                                "Brevo send failed: " + resp.statusCode() + " " + body))))
                .toBodilessEntity()
                .then();
    }
}
