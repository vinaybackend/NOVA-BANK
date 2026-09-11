package com.nova.bank.kyc.securityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final RoleConverter roleConverter;

    public SecurityConfig(RoleConverter roleConverter) {
        this.roleConverter = roleConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/actuator/**")
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/kycs/review/*",
                                "/api/v1/kycs/approve/*",
                                "/api/v1/kycs/*/reject/*"
                        ).hasRole("KYC_OFFICER")
                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> {
                            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

                            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(roleConverter);

                            jwt.jwtAuthenticationConverter(jwtAuthenticationConverter);
                        })
                );



        return http.build();
    }
}
