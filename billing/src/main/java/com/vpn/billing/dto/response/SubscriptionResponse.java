package com.vpn.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long userId;
    private Boolean active;
    private LocalDateTime expiresAt;
    private Long daysLeft;
}
