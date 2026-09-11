package com.nova.bank.securityConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Autowired
    private RoleConverter roleConverter;
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity security){
        security.cors(Customizer.withDefaults())
                .csrf(csrfSpec -> csrfSpec.disable())
                .authorizeExchange(exchange->exchange
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(HttpMethod.GET).hasRole("BANK_EMPLOYEE")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(config->config.jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(roleExtract())));
        return security.build();
    }

    private Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> roleExtract() {
        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(roleConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}

