package com.vpn.auth.service.refresh;

import com.vpn.auth.entity.RefreshToken;
import com.vpn.auth.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyAndRevoke(String refreshToken);
    void revokeAllUserTokens(Long userId);
}
