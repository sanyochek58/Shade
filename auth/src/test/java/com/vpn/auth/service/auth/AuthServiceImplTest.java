package com.vpn.auth.service.auth;

import com.vpn.auth.dto.request.LoginRequest;
import com.vpn.auth.dto.request.RegisterRequest;
import com.vpn.auth.dto.response.AuthResponse;
import com.vpn.auth.entity.RefreshToken;
import com.vpn.auth.entity.Role;
import com.vpn.auth.entity.User;
import com.vpn.auth.exception.AuthException;
import com.vpn.auth.kafka.UserEventProducer;
import com.vpn.auth.repository.UserRepository;
import com.vpn.auth.service.jwt.JwtService;
import com.vpn.auth.service.refresh.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    private AuthServiceImpl authServiceImpl;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserEventProducer userEventProducer;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        authServiceImpl = new AuthServiceImpl(
                userRepository,
                refreshTokenService,
                jwtService,
                passwordEncoder = new BCryptPasswordEncoder(4),
                userEventProducer,
                new SimpleMeterRegistry()
        );
    }
    
    // Тесты с регистрацией 
    @Test
    @DisplayName("Тест: Регистрация с занятым email - выбрасывает AuthException")
    public void register_emailAlreadyExists_throwsAuthException(){

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@email.com");
        registerRequest.setPassword("passwordpassword");

        when(userRepository.existsByEmail("user@email.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authServiceImpl.register(registerRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("уже существует");

        verify(userRepository, never()).save(any());
        verify(userEventProducer, never()).sendUserRegistered(any(), any());

    }

    @Test
    @DisplayName("Тест: Регистрация - отправляется событие в kafka")
    public void register_email_SendEventKafka(){
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@email.com");
        registerRequest.setPassword("passwordpassword");

        when(userRepository.existsByEmail("user@email.com")).thenReturn(false);

        User savedUser = buildUser(1L, "user@email.com", false);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getAccessExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn(buildRefreshToken(savedUser));

        authServiceImpl.register(registerRequest);

        verify(userEventProducer, times(1)).sendUserRegistered(any(), any());
    }

    @Test
    @DisplayName("Тест: Регистрация с валидными данными")
    public void register_with_validCredentials(){
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@email.com");
        registerRequest.setPassword("passwordpassword");

        when(userRepository.existsByEmail("user@email.com")).thenReturn(false);

        User savedUser = buildUser(1L, "user@email.com", false);
        when(userRepository.save(any())).thenReturn(savedUser);

        when(jwtService.generateToken(any(User.class))).thenReturn("test-access-token");
        when(jwtService.getAccessExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(buildRefreshToken(savedUser));

        AuthResponse authResponse = authServiceImpl.register(registerRequest);

        assertThat(authResponse.getAccessToken()).isEqualTo("test-access-token");
        assertThat(authResponse.getRefreshToken()).isEqualTo("test-refresh-uuid");
        assertThat(authResponse.getExpiresIn()).isEqualTo(900L);
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
    }


    @Test
    @DisplayName("Регистрация — пользователь сохраняется в БД")
    void register_validData_savedUserToDatabase() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@shade.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        User savedUser = buildUser(1L, "test@shade.com", false);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getAccessExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn(buildRefreshToken(savedUser));
        
        authServiceImpl.register(request);
        
        verify(userRepository, times(1)).save(any(User.class));
    }

    //Тесты с логином
    @Test
    @DisplayName("Тест: Логин с валидными данными")
    public void test_login_with_validCredentials() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@shade.com");
        loginRequest.setPassword("password123");

        User user = buildUser(1L, "test@shade.com", false);
        user.setPassword(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("test@shade.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(jwtService.getAccessExpiration()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any()))
                .thenReturn(buildRefreshToken(user));

        AuthResponse authResponse = authServiceImpl.login(loginRequest);

        assertThat(authResponse.getAccessToken()).isEqualTo("access-token");
        assertThat(authResponse.getRefreshToken()).isEqualTo("test-refresh-uuid");
    }

    @Test
    @DisplayName("Тест: Логин с несуществующим email")
    public void test_login_with_invalidEmail(){
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@shade.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail("test@shade.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authServiceImpl.login(loginRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Неверный Email или пароль !");
    }

    //Тесты с логаутом
    @Test
    @DisplayName("Тест: Логаут")
    public void test_logout(){
        authServiceImpl.logout(1L);
        verify(refreshTokenService, times(1)).revokeAllUserTokens(1L);
    }
    
    //Вспомогательные методы
    private User buildUser(Long id, String email, boolean isAdmin) {
        return User.builder()
                .id(id)
                .email(email)
                .password("hashed_password")
                .role(isAdmin ? Role.ROLE_ADMIN : Role.ROLE_USER)
                .active(true)
                .build();
    }

    private RefreshToken buildRefreshToken(User user) {
        return RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("test-refresh-uuid")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
    }
}
