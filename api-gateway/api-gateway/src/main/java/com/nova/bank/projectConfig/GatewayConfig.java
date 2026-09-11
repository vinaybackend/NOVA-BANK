package com.nova.bank.projectConfig;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator routes(RouteLocatorBuilder routeLocatorBuilder){
        return routeLocatorBuilder.routes()
                .route("customer-service",route->route.path("/customer/**")
                        .filters(f->f.rewritePath("/customer/?(?<remaining>.*)","/${remaining}"))
                        .uri("lb://CUSTOMER-SERVICE")).build();
    }
}
