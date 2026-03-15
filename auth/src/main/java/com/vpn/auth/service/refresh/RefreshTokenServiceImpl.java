package com.vpn.auth.service.refresh;

import com.vpn.auth.entity.RefreshToken;
import com.vpn.auth.entity.User;
import com.vpn.auth.exception.TokenException;
import com.vpn.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days:30}")
    private int refreshExpirationDays;

    @Transactional
    @Override
    public RefreshToken createRefreshToken(User user){
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken verifyAndRevoke(String token){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("Refresh токен не найден !"));

        if(refreshToken.getRevoked()){
            throw new TokenException("Токен уже использовался !");
        }

        if(refreshToken.isExpired()){
            throw new TokenException("Refresh token уже истёк, войдите снова !");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId){
        refreshTokenRepository.deleteByUserId(userId);
    }

}
