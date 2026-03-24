package com.vpn.vpn_conf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnConfigResponse {
    private Long userId;
    private String vlessLink;
    private Boolean active;
    private LocalDateTime createdAt;
}
