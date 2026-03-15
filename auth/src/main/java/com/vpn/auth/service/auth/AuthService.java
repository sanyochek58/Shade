package com.vpn.auth.service.auth;

import com.vpn.auth.dto.request.LoginRequest;
import com.vpn.auth.dto.request.RefreshRequest;
import com.vpn.auth.dto.request.RegisterRequest;
import com.vpn.auth.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshRequest request);
    void logout(Long userId);
}
