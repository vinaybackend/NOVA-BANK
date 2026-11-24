package com.nova.bank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Maps the URL /images/customers/** to the physical folder 'uploads/customers/'
        registry.addResourceHandler("/images/customers/**")
                .addResourceLocations("file:uploads/customers/");
    }
}
