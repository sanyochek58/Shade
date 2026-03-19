package com.vpn.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentResponse {
    private String payUrl;
    private BigDecimal amount;
    private String currency;
}
