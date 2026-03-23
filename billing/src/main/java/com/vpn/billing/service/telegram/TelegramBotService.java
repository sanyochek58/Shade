package com.vpn.billing.service.telegram;

import com.vpn.billing.service.billing.BillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${telegram.bot-token:}')")
public class TelegramBotService implements SpringLongPollingBot {

    private final TelegramClient telegramClient;
    private final BillingService billingService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.bot-username}")
    private String botUsername;

    public TelegramBotService(@Value("${telegram.bot-token}")  String botToken, @Lazy BillingService billingService) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.billingService = billingService;
    }

    //Обязательные методы для SpringLongPollingBot
    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer(){
        return updates -> updates.forEach(this::handleUpdate);
    }


    // Создание счёта на оплату — прямой HTTP-вызов т.к. telegrambots 7.2.1 не допускает пустой providerToken (баг для XTR)
    @SuppressWarnings("unchecked")
    public String createInvoiceLink(Long userId, Integer stars) {
        String url = "https://api.telegram.org/bot" + botToken + "/createInvoiceLink";

        Map<String, Object> body = Map.of(
                "title", "VPN подписка - 30 дней",
                "description", "Безлимитный доступ + раздельное туннелирование",
                "payload", "vpn_sub_" + userId,
                "currency", "XTR",
                "provider_token", "",
                "prices", List.of(Map.of("label", "VPN 30 дней", "amount", stars))
        );

        Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);

        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            log.error("Telegram API вернул ошибку: {}", response);
            throw new RuntimeException("Не удалось создать платёж: " + response);
        }

        String link = (String) response.get("result");
        log.info("Создана ссылка на оплату для userId={}", userId);
        return link;
    }

    // Обработка обновлений от тг
    private void handleUpdate(Update update) {
        if (update.hasMessage()) {
            if(update.getMessage().hasText() && update.getMessage().getText().startsWith("/start")) {
                handleStart(update);
            }

            if(update.getMessage().hasSuccessfulPayment()){
                handleSuccessfulPayment(update);
            }
        }

        if(update.hasPreCheckoutQuery()){
            handlePreCheckout(update);
        }
    }

    private void handleStart(Update update) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getFrom().getFirstName();

        sendMessage(chatId, "Привет, " + firstName + "!\n\n" +
                "Добро пожаловать в Shade VPN.\n" +
                "Для оплаты подписки используй приложение.");
    }

    private void handlePreCheckout(Update update) {
        try {
            telegramClient.execute(
                    org.telegram.telegrambots.meta.api.methods
                            .AnswerPreCheckoutQuery.builder()
                            .preCheckoutQueryId(update.getPreCheckoutQuery().getId())
                            .ok(true)
                            .build()
            );
            log.info("PreCheckout подтверждён");
        } catch (TelegramApiException e) {
            log.error("Ошибка PreCheckout: {}", e.getMessage());
        }
    }

    private void handleSuccessfulPayment(Update update) {
        SuccessfulPayment payment = update.getMessage().getSuccessfulPayment();
        Long telegramUserId = update.getMessage().getFrom().getId();

        log.info("💰 Успешная оплата! payload={}, chargeId={}",
                payment.getInvoicePayload(),
                payment.getTelegramPaymentChargeId());

        String payload = payment.getInvoicePayload();
        Long userId = Long.parseLong(payload.replace("vpn_sub_", ""));

        billingService.activateSubscription(
                userId,
                payment.getTelegramPaymentChargeId(),
                payment.getTotalAmount()
        );

        sendMessage(update.getMessage().getChatId(),
                "🎉 Оплата прошла успешно!\n\n" +
                        "Подписка активна на 30 дней.\n" +
                        "Откройте приложение Shade VPN для подключения."
        );
    }

    private void sendMessage(Long chatId, String text) {
        try {
            telegramClient.execute(
                    SendMessage.builder()
                            .chatId(chatId.toString())
                            .text(text)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }
}
