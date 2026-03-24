package com.vpn.vpn_conf.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.vpn_conf.service.vpnService.VpnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final VpnService vpnService;

    @KafkaListener(
            topics = "payments-event",
            groupId = "vpn-service-group"
    )
    public void handlePaymentEvent(String message) {
        try{
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String type = payload.get("type").toString();
            Long userId = Long.valueOf(payload.get("userId").toString());

            log.info("Получено сообщение из Kafka: type={} ", type);

            switch (type) {
                case "PAYMENT_SUCCESS" -> vpnService.createVpnConfig(userId);
                case "SUBSCRIPTION_EXPIRED" -> vpnService.deactivateVpnConfig(userId);
                default -> log.warn("Неизвестный тип события: {}", type);
            }
        }catch (Exception e){
            log.error("Ошибка обработки payment-event: {}", e.getMessage(), e);
        }
    }
}
