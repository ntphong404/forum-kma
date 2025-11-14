package com.forum.kma.notification.service;

import com.forum.kma.notification.dto.EmailRequest;
import reactor.core.publisher.Mono;

public interface EmailSender {
    Mono<Void> send(EmailRequest req);
}
