package com.nova.bank.projectConfig;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder routeLocatorBuilder) {

        return routeLocatorBuilder.routes()

                .route("auth-service", route -> route.path("/auth-service/**").filters(f -> f.rewritePath("/auth-service/(?<remaining>.*)", "/${remaining}")
                         .retry(retryConfig -> retryConfig.setRetries(3).setBackoff(Duration.ofMillis(50),Duration.ofMillis(600),2,true)))
                        .uri("lb://AUTH-SERVICE"))

                .route("customer-service", route -> route.path("/customer-service/**").filters(f -> f.rewritePath("/customer-service/(?<remaining>.*)", "/${remaining}").circuitBreaker(c->c.setName("customerServiceCircuitBreaker").setFallbackUri("forward:/fallback"))).uri("lb://CUSTOMER-SERVICE"))

                .route("account-service", route -> route.path("/account-service/**").filters(f -> f.rewritePath("/account-service/(?<remaining>.*)", "/${remaining}")).uri("lb://ACCOUNT-SERVICE"))

                .route("transaction-service", route -> route.path("/transaction-service/**").filters(f -> f.rewritePath("/transaction-service/(?<remaining>.*)", "/${remaining}")).uri("lb://TRANSACTION-SERVICE"))

                .route("kyc-service", route -> route.path("/kyc-service/**").filters(f -> f.rewritePath("/kyc-service/(?<remaining>.*)", "/${remaining}")).uri("lb://KYC-SERVICE"))

                .route("branch-service", route -> route.path("/branch-service/**").filters(f -> f.rewritePath("/branch-service/(?<remaining>.*)", "/${remaining}")).uri("lb://BRANCH-SERVICE"))

                .route("employee-service", route -> route.path("/employee-service/**").filters(f -> f.rewritePath("/employee-service/(?<remaining>.*)", "/${remaining}")).uri("lb://EMPLOYEE-SERVICE"))

                // Loan Service
                .route("loan-service", route -> route.path("/loan-service/**").filters(f -> f.rewritePath("/loan-service/(?<remaining>.*)", "/${remaining}")).uri("lb://LOAN-SERVICE"))

                .build();
    }
}