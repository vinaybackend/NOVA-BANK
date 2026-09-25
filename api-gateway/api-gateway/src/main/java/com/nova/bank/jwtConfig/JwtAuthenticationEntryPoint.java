package com.nova.bank.jwtConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint
        implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> commence(org.springframework.web.server.ServerWebExchange exchange, AuthenticationException ex) {

        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();

        errorResponse.put("status", 401);
        errorResponse.put("error", "UNAUTHORIZED");
        errorResponse.put("message", "Access token has expired");

        try {

            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);

            var buffer = response.bufferFactory().wrap(bytes);

            return response.writeWith(
                    Mono.just(buffer)
            );

        } catch (JsonProcessingException e) {

            return Mono.error(e);
        }
    }
}
