package com.vpn.billing.service.billing;

import com.vpn.billing.dto.response.PaymentResponse;
import com.vpn.billing.dto.response.SubscriptionResponse;

public interface BillingService {
    PaymentResponse initiatePayment(Long userId);

    void activateSubscription(Long userId, String telegramChargeId, Integer stars);

    SubscriptionResponse getSubscription(Long userId);

}
