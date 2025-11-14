package com.forum.kma.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.event.PostEvent;
import com.forum.kma.notification.dto.EmailRequest;
import com.forum.kma.notification.dto.Recipient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostEventConsumer {

    private final KafkaReceiver<String, String> postKafkaReceiver;
    private final EmailSender emailSender;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void consume() {
        postKafkaReceiver.receive()
                .flatMap(record ->
                                Mono.fromCallable(() -> mapper.readValue(record.value(), PostEvent.class))
                                        .flatMap(event -> {
                                            EmailRequest email = new EmailRequest(
                                                    List.of(new Recipient("John Dev", "devteria@yopmail.com")),
                                                    "New post: " + event.getTitle(),
                                                    "<p>New post published: " + event.getTitle() + "</p>"
                                            );
                                            return emailSender.send(email)
                                                    .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge()));
                                        })
                                        .onErrorResume(e -> {
                                            log.error("Error processing post record: {}", e.getMessage(), e);
                                            try { record.receiverOffset().acknowledge(); } catch (Exception ex) { /* ignore */ }
                                            return Mono.empty();
                                        })
                        , 8)
                .subscribe();
    }
}
