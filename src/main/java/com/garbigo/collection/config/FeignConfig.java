package com.garbigo.collection.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Bean
    RequestInterceptor internalApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header(INTERNAL_API_KEY_HEADER, internalApiKey);
    }
}