package com.vpn.billing.controller;

import com.vpn.billing.dto.response.PaymentResponse;
import com.vpn.billing.dto.response.SubscriptionResponse;
import com.vpn.billing.service.billing.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/billing")
@Slf4j
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestHeader("X-User-Id") Long userId){
        log.info("→ POST /billing/pay userId={}", userId);

        PaymentResponse paymentResponse = billingService.initiatePayment(userId);
        return ResponseEntity.ok(paymentResponse);
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionResponse> getSubscription(@RequestHeader("X-User-Id") Long userId){
        log.info("→ GET /billing/subscription userId={}", userId);

        SubscriptionResponse subscriptionResponse = billingService.getSubscription(userId);
        return ResponseEntity.ok(subscriptionResponse);
    }
}
