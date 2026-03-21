package com.vpn.billing.kafka;

import com.vpn.billing.entity.Subscription;
import com.vpn.billing.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final ObjectMapper objectMapper;
    private final SubscriptionRepository subscriptionRepository;

    @KafkaListener(
            topics = "user-events",
            groupId = "billing-service-group"
    )
    public void handleUserEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            String type = (String) event.get("type");
            log.info("Получено сообщение из Kafka: type={} ", type);

            if ("USER_REGISTERED".equals(type)) {
                handleUserRegistered(event);
            }

        } catch (Exception e) {
            log.error("Ошибка обработки user-event: {}", e.getMessage(), e);
        }
    }

    public void handleUserRegistered(Map<String, Object> event) {
        Long userId = Long.valueOf(event.get("userId").toString());
        String email = String.valueOf(event.get("email"));

        log.info("Новый пользователь: userId={}, email={}", userId, email);

        if (subscriptionRepository.existsByUserId(userId)) {
            log.warn("⚠️ Подписка для userId={} уже существует", userId);
            return;
        }

        Subscription subscription = Subscription.builder()
                .userId(userId)
                .active(false)
                .build();

        subscriptionRepository.save(subscription);
        log.info("Создана подписка для userId={}", userId);
    }
}

