package com.nova.bank.transaction.clients;

import com.nova.bank.transaction.exceptions.InsufficientBalanceException;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {

        return requestTemplate -> {

            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {

                String token = jwtAuthentication.getToken().getTokenValue();

                requestTemplate.header(
                        "Authorization",
                        "Bearer " + token
                );
            }
        };
    }


        @Bean
        public ErrorDecoder errorDecoder() {
            return (methodKey, response) -> {
                if (response.status() == 400) {
                    // parse the body to distinguish INSUFFICIENT_BALANCE vs other 400s
                    return new InsufficientBalanceException("Insufficient balance");
                }
                return new ErrorDecoder.Default().decode(methodKey, response);
            };
        }
    }

