package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${services.auth-url}")
    private String authUrl;

    @Value("${services.billing-url}")
    private String billingUrl;

    @Value("${services.vpn-url}")
    private String vpnUrl;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri(authUrl))
                .route("billing-service", r -> r
                        .path("/api/billing/**")
                        .uri(billingUrl))
                .route("vpn-service", r -> r
                        .path("/api/vpn/**")
                        .uri(vpnUrl))
                .build();
    }
}
