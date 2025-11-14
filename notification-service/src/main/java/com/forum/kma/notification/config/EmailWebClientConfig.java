package com.forum.kma.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class EmailWebClientConfig {

    private final EmailProperties emailProperties;

    @Bean("brevoWebClient")
    public WebClient brevoWebClient(WebClient.Builder builder) {
        EmailProperties.Brevo brevo = emailProperties.getBrevo();
        WebClient.Builder b = builder
                .baseUrl(brevo.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (brevo.getApiKey() != null && !brevo.getApiKey().isBlank()) {
            b.defaultHeader("api-key", brevo.getApiKey());
        }

        return b.build();
    }
}
