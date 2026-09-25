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
import reactor.core.publisher.Mono;

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
                                "/auth-service/api/v1/admin/auth/refresh-token")
                        .permitAll()
                        .pathMatchers("/auth-service/api/v1/admin/auth/**","/api/v1/branches/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/customers/**","/api/v1/kycs/**").hasAnyRole("CUSTOMER","ADMIN","BRANCH_MANAGER","KYC_OFFICER","TELLER")
                        .pathMatchers("/api/v1/accounts").hasAnyRole("BRANCH_MANAGER")
                        .pathMatchers("/api/v1/accounts/internal/**").hasAnyRole("ADMIN","TELLER","BRANCH_MANAGER")
                        .pathMatchers("/api/v1/transactions").hasAnyRole("BRANCH_MANAGER","TELLER")

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

}

