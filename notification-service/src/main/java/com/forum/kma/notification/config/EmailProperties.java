package com.forum.kma.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public class EmailProperties {
    private Brevo brevo = new Brevo();

    @Data
    public static class Brevo {
        private String url = "https://api.brevo.com/v3/smtp/email";
        private String apiKey;
        private Sender sender = new Sender();

        @Data
        public static class Sender {
            private String name;
            private String email;
        }
    }
}
