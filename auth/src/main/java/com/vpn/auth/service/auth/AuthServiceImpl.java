package com.vpn.auth.service.auth;

import com.vpn.auth.dto.request.LoginRequest;
import com.vpn.auth.dto.request.RefreshRequest;
import com.vpn.auth.dto.request.RegisterRequest;
import com.vpn.auth.dto.response.AuthResponse;
import com.vpn.auth.entity.RefreshToken;
import com.vpn.auth.entity.Role;
import com.vpn.auth.entity.User;
import com.vpn.auth.exception.AuthException;
import com.vpn.auth.kafka.UserEventProducer;
import com.vpn.auth.repository.RefreshTokenRepository;
import com.vpn.auth.repository.UserRepository;
import com.vpn.auth.service.jwt.JwtService;
import com.vpn.auth.service.refresh.RefreshTokenService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;

    //Метрики prometheus
    private final Counter registrationCounter;
    private final Counter loginCounter;
    private final Counter loginFailedCounter;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserEventProducer userEventProducer,
            MeterRegistry meterRegistry
    ){
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userEventProducer = userEventProducer;

        this.registrationCounter = Counter.builder("auth.registrations.total").description("Количество регистраций").register(meterRegistry);
        this.loginCounter = Counter.builder("auth.logins.success.total").description("Успешные логины").register(meterRegistry);
        this.loginFailedCounter = Counter.builder("auth.logins.failed.total").description("Не успешные попытки логина").register(meterRegistry);

    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new AuthException("Пользователь с таким email уже существует !");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        userRepository.save(user);
        log.info("Новый пользователь зарегистрирован: {} ", user.getEmail());

        userEventProducer.sendUserRegistered(user.getId(), user.getEmail());

        registrationCounter.increment();

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> {
                    loginFailedCounter.increment();
                    return new AuthException("Неверный Email или пароль !");
                }
        );

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            loginFailedCounter.increment();
            throw new AuthException("Неверный Email или пароль !");
        }

        if (!user.isActive()) {
            throw new AuthException("Аккаунт заблокирован !");
        }
        loginCounter.increment();
        log.info("Логин: {}",  user.getEmail());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request){
        RefreshToken refreshToken = refreshTokenService.verifyAndRevoke(request.getRefreshToken());
        return buildAuthResponse(refreshToken.getUser());
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("Логаут userId = {}", userId);
    }


    private AuthResponse buildAuthResponse(User user){
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getAccessExpiration()/1000)
                .build();
    }
}
