package com.estapar.parking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Beans de infraestrutura compartilhados.
 */
@Configuration
@EnableConfigurationProperties(SimulatorProperties.class)
public class AppConfig {

    /**
     * Cliente HTTP usado para consultar o simulador da garagem.
     */
    @Bean
    public RestClient simulatorRestClient(SimulatorProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(clientRequestFactory())
                .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory clientRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return factory;
    }
}
