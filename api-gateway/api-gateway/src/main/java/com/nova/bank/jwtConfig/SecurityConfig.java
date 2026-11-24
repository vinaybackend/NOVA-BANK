package com.nova.bank.jwtConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Autowired
    private RoleConverter roleConverter;
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity security){
        security.cors(Customizer.withDefaults())
                .csrf(csrfSpec -> csrfSpec.disable())
                .authorizeExchange(exchange->exchange
                        .pathMatchers("/actuator/**",
                                "/auth-service/api/v1/auth/register",
                                "/auth-service/api/v1/auth/login",
                                "/auth-service/api/v1/admin/auth/login",
                                "/auth-service/api/v1/auth/refresh-token",
                                "/auth-service/api/v1/admin/auth/refresh-token",
                                "/branch-service/api/v1/branches/{branchCode}",
                                "/customer-service/api/v1/customers/create",
                                "/kyc-service/api/v1/kyc/submit/documents",
                                "/branch-service/api/v1/branches",
                                "/loan-service/api/v1/loan-products")
                        .permitAll()
                        .pathMatchers("/auth-service/**").hasAnyRole("ADMIN","CUSTOMER","BRANCH_MANAGER","KYC_OFFICER","TELLER")
                        .pathMatchers("/customer-service/**").hasAnyRole("CUSTOMER","ADMIN","BRANCH_MANAGER","KYC_OFFICER","TELLER")
                        .pathMatchers("/account-service/**").hasAnyRole("CUSTOMER","ADMIN","BRANCH_MANAGER","KYC_OFFICER","TELLER","LOAN_OFFICER")
                        .pathMatchers("/transaction-service/**").hasAnyRole("CUSTOMER","ADMIN","BRANCH_MANAGER","KYC_OFFICER","TELLER")
                        .pathMatchers("/kyc-service/**").hasAnyRole("CUSTOMER","ADMIN","BRANCH_MANAGER","KYC_OFFICER","TELLER")
                        .pathMatchers("/employee-service/**").hasAnyRole("ADMIN","BRANCH_MANAGER","HR")
                        .pathMatchers("/branch-service/**").hasAnyRole("ADMIN","BRANCH_MANAGER","HR","KYC_OFFICER","TELLER","CUSTOMER","HR")
                        .pathMatchers("/loan-service/**").hasAnyRole("CUSTOMER","ADMIN","BRANCH_MANAGER","KYC_OFFICER","TELLER","LOAN_OFFICER")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(config->config.jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(roleExtract()))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint));
        return security.build();
    }

    private Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> roleExtract() {
        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(roleConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    @Bean
    public CorsConfigurationSource configurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
