package com.example.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Component
@Order(0)
@Slf4j
public class ProxyFilter implements WebFilter {

    private final WebClient webClient;
    private final Map<String, String> routes;

    public ProxyFilter(
            WebClient webClient,
            @Value("${services.auth-url}") String authUrl,
            @Value("${services.billing-url}") String billingUrl,
            @Value("${services.vpn-url}") String vpnUrl
    ) {
        this.webClient = webClient;
        this.routes = Map.of(
                "/api/auth", authUrl,
                "/api/billing", billingUrl,
                "/api/vpn", vpnUrl
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        String targetBase = routes.entrySet().stream()
                .filter(e -> path.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (targetBase == null) {
            return chain.filter(exchange);
        }

        URI targetUri;
        try {
            String rawQuery = exchange.getRequest().getURI().getRawQuery();
            String uriStr = targetBase + path + (rawQuery != null ? "?" + rawQuery : "");
            targetUri = new URI(uriStr);
        } catch (URISyntaxException e) {
            log.error("Ошибка построения URI: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }

        HttpHeaders forwardHeaders = new HttpHeaders();
        exchange.getRequest().getHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase(HttpHeaders.HOST) &&
                !name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                forwardHeaders.addAll(name, values);
            }
        });

        return webClient.method(exchange.getRequest().getMethod())
                .uri(targetUri)
                .headers(h -> h.addAll(forwardHeaders))
                .body(exchange.getRequest().getBody(), DataBuffer.class)
                .exchangeToMono(clientResponse -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(clientResponse.statusCode());
                    clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                        if (!name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                            response.getHeaders().addAll(name, values);
                        }
                    });
                    return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                });
    }
}
