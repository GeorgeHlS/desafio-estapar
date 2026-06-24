package com.estapar.parking.client;

import com.estapar.parking.client.dto.GarageConfigResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente para o simulador da garagem
 */
@Component
public class GarageSimulatorClient {

    private final RestClient restClient;

    public GarageSimulatorClient(RestClient simulatorRestClient) {
        this.restClient = simulatorRestClient;
    }

    /**
     * Busca a configuração da garagem no simulador.
     *
     * @return setores e vagas configurados
     */
    public GarageConfigResponse fetchGarageConfig() {
        return restClient.get()
                .uri("/garage")
                .retrieve()
                .body(GarageConfigResponse.class);
    }
}
