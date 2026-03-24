package com.vpn.billing.service.billing;

import com.vpn.billing.dto.response.PaymentResponse;
import com.vpn.billing.dto.response.SubscriptionResponse;
import com.vpn.billing.entity.Payment;
import com.vpn.billing.entity.PaymentStatus;
import com.vpn.billing.entity.Subscription;
import com.vpn.billing.exception.BillingException;
import com.vpn.billing.kafka.PaymentEventProducer;
import com.vpn.billing.repository.PaymentsRepository;
import com.vpn.billing.repository.SubscriptionRepository;
import com.vpn.billing.service.telegram.TelegramBotService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentsRepository paymentsRepository;
    private final Optional<TelegramBotService> telegramBotService;
    private final PaymentEventProducer paymentEventProducer;

    @Value("${vpn.price-stars:110}")
    private Integer priceStars;

    private final Counter paymentSuccessCounter;
    private final Counter paymentInitiatedCounter;

    public BillingServiceImpl(
            SubscriptionRepository subscriptionRepository,
            PaymentsRepository paymentsRepository,
            @Autowired(required = false) TelegramBotService telegramBotService,
            PaymentEventProducer paymentEventProducer,
            MeterRegistry meterRegistry
    ){
        this.subscriptionRepository = subscriptionRepository;
        this.paymentsRepository = paymentsRepository;
        this.telegramBotService = Optional.ofNullable(telegramBotService);
        this.paymentEventProducer = paymentEventProducer;

        this.paymentSuccessCounter = Counter.builder("billing.payments.success.total")
                .description("Успешные платежи")
                .register(meterRegistry);

        this.paymentInitiatedCounter = Counter.builder("billing.payments.initiated.total")
                .description("Инициированные платежи")
                .register(meterRegistry);

    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long userId) {
        log.info("Инициируем платёж для userId={}", userId);

        Subscription subscription = subscriptionRepository.findByUserId(userId).orElseThrow(() -> new BillingException(
                "Подписка не найдена для userId=" + userId
        ));

        if(subscription.isCurrentlyActive()){
            log.info(" Подписка уже активна для userId={}", userId);
        }

        Payment payment = Payment.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(priceStars))
                .currency("XTR")
                .status(PaymentStatus.PENDING)
                .build();
        paymentsRepository.save(payment);

        String payUrl = telegramBotService
                .orElseThrow(() -> new BillingException("Telegram bot не настроен. Укажите TELEGRAM_BOT_TOKEN."))
                .createInvoiceLink(userId, priceStars);
        paymentInitiatedCounter.increment();
        log.info("Платёж создан: userId={}, stars={}", userId, priceStars);

        return PaymentResponse.builder()
                .payUrl(payUrl)
                .amount(BigDecimal.valueOf(priceStars))
                .currency("XTR")
                .build();
    }

    @Override
    @Transactional
    public void activateSubscription(Long userId, String telegramChargeId, Integer stars){
        log.info("Активируем подписку: userId={}, chargeId={}", userId, telegramChargeId);

        paymentsRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, PaymentStatus.PENDING).ifPresent( payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTelegramChargeId(telegramChargeId);
            payment.setPaidAt(LocalDateTime.now());
            paymentsRepository.save(payment);
        });

        Subscription subscription = subscriptionRepository.findByUserId(userId).orElse(Subscription.builder().userId(userId).build());

        LocalDateTime base = (subscription.isCurrentlyActive())
                ? subscription.getExpiresAt()
                : LocalDateTime.now();

        subscription.setActive(true);
        subscription.setExpiresAt(base.plusDays(30));
        subscriptionRepository.save(subscription);

        paymentEventProducer.sendPaymentSuccess(userId);

        paymentSuccessCounter.increment();
        log.info("🎉 Подписка активна до: {}", subscription.getExpiresAt());
    }

    @Override
    public SubscriptionResponse getSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId).orElseThrow(() -> new BillingException("Подписка не найдена для userId=" + userId));

        Long daysLeft = null;
        if(subscription.isCurrentlyActive()){
            daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), subscription.getExpiresAt());
        }
        return SubscriptionResponse.builder()
                .userId(userId)
                .active(subscription.isCurrentlyActive())
                .expiresAt(subscription.getExpiresAt())
                .daysLeft(daysLeft)
                .build();
    }


}
