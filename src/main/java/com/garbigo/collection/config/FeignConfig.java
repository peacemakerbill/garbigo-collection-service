package com.garbigo.collection.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Attaches the shared INTERNAL_API_KEY to every outgoing Feign call (to
 * auth-service now, to wallet-service once it exists), the same way
 * auth-service's InternalApiKeyFilter expects it on the way in.
 *
 * <p>This bean is picked up globally by Spring Cloud OpenFeign's default
 * auto-configuration (any {@code RequestInterceptor} bean in the
 * application context is applied to every {@code @FeignClient}), so it
 * doesn't need to be wired into {@code AuthServiceClient} or
 * {@code WalletServiceClient} individually.
 */
@Configuration
public class FeignConfig {

    @Value("${garbigo.internal-api.header-name}")
    private String internalApiKeyHeaderName;

    @Value("${garbigo.internal-api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header(internalApiKeyHeaderName, internalApiKey);
    }
}
