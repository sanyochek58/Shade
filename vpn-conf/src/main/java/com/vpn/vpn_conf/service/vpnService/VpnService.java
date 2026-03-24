package com.vpn.vpn_conf.service.vpnService;

import com.vpn.vpn_conf.dto.VpnConfigResponse;

public interface VpnService {
    void createVpnConfig(Long userId);
    void deactivateVpnConfig(Long userId);
    VpnConfigResponse getVpnConfig(Long userId);
}
