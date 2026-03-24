package com.vpn.auth.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "user-events";

    public void sendUserRegistered(Long userId, String email) {
        try{
            Map<String, Object> event = Map.of(
                    "type", "USER_REGISTERED",
                    "userId", userId,
                    "email", email,
                    "timestamp", System.currentTimeMillis()
            );

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, message);

            log.info("KAFKA -> USER_REGISTERED: userId = {}, email = {}", userId, email);
        }catch (Exception e){
            log.error("Ошибка отправки события в KAFKA: {}", e.getMessage());
        }
    }
}
