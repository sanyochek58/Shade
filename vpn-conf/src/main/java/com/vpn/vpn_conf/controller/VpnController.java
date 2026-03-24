package com.vpn.vpn_conf.controller;

import com.vpn.vpn_conf.dto.VpnConfigResponse;
import com.vpn.vpn_conf.service.vpnService.VpnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vpn")
@RequiredArgsConstructor
@Slf4j
public class VpnController {

    private final VpnService vpnService;

    @GetMapping("/config")
    public ResponseEntity<VpnConfigResponse> getConfig(
            @RequestHeader("X-User-Id") Long userId
    ){
        log.info("→ GET /vpn/config userId={}", userId);
        return ResponseEntity.ok(vpnService.getVpnConfig(userId));
    }
}
