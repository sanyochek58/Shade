package com.vpn.billing.service.telegram;

import com.vpn.billing.service.billing.BillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Service
@Slf4j
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${telegram.bot-token:}')")
public class TelegramBotService implements SpringLongPollingBot {

    private final TelegramClient telegramClient;
    private final BillingService billingService;

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


    // Создание счёта на оплату
    public String createInvoiceLink(Long userId, Integer stars) {
        try{
            CreateInvoiceLink invoiceLink = CreateInvoiceLink.builder()
                    .title("VPN подписка - 30 дней")
                    .description("Безлимитный доступ + раздельныое туннелирование")
                    .payload("vpn_sub_" + userId)
                    .currency("XTR")
                    .prices(List.of(
                            new org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice("VPN 30 дней", stars)
                    ))
                    .providerToken("")
                    .build();

            String link = telegramClient.execute(invoiceLink);

            log.info("Создана ссылка на оплату для userId={}", userId);
            return link;
        } catch (TelegramApiException e) {
            log.error("Ошибка создания инвойса: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать платёж", e);
        }
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
