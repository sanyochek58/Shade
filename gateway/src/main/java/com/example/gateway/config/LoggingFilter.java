package com.example.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class LoggingFilter {

    @Bean
    @Order(-1)
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getPath().value();

            log.info("→ {} {}", method, path);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                int status = exchange.getResponse().getStatusCode().value();
                log.info("← {} {} [{}]", method, path, status);
            }));
        };
    }
}
