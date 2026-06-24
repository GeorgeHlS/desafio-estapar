package com.estapar.parking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração do simulador da garagem
 */
@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        String baseUrl,
        boolean bootstrapEnabled
) {
}
