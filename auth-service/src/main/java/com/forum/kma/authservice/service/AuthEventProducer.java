package com.forum.kma.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.event.AuthEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Service
@RequiredArgsConstructor
public class AuthEventProducer {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> sendAuthEvent(AuthEvent event) {
        try {
            String value = objectMapper.writeValueAsString(event);
            SenderRecord<String, String, String> record =
                    SenderRecord.create("auth-topic", null, null, event.getUserId(), value, event.getUserId());

            return kafkaSender.send(Mono.just(record)).then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
