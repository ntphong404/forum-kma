package com.forum.kma.postservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.event.PostEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Service
@RequiredArgsConstructor
public class PostEventProducer {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> sendPostEvent(PostEvent event) {
        try {
            String value = objectMapper.writeValueAsString(event);
            // Dùng event.getId() làm correlationMetadata để không null
            SenderRecord<String, String, String> record =
                    SenderRecord.create("post-topic", null, null, event.getId(), value, event.getId());

            // Gửi record
            return kafkaSender.send(Mono.just(record))
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
