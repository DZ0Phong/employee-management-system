package com.group5.ems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * This tells Spring Boot how to create the RestClient.Builder
     * so it can be injected into your VietQrApiClient.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Optional: If any other service just wants the RestClient directly
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
