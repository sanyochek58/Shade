package com.vpn.billing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "payments-events";

    public void sendPaymentSuccess(Long userId) {
        try {
            Map<String, Object> event = Map.of(
                    "type", "PAYMENT_SUCCESS",
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
            );
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, userId.toString(), message);

            log.info("Kafka → PAYMENT_SUCCESS: userId={}", userId);
        } catch (Exception e) {
            log.error("Ошибка отправки в Kafka: {}", e.getMessage(), e);
        }
    }

    public void sendSubscriptionExpired(Long userId) {
        try {
            Map<String, Object> event = Map.of(
                    "type", "SUBSCRIPTION_EXPIRED",
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
            );
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, userId.toString(), message);

            log.info("Kafka → SUBSCRIPTION_EXPIRED: userId={}", userId);
        } catch (Exception e) {
            log.error("Ошибка отправки в Kafka: {}", e.getMessage(), e);
        }
    }
}
