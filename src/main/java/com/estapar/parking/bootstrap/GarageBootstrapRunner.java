package com.estapar.parking.bootstrap;

import com.estapar.parking.config.SimulatorProperties;
import com.estapar.parking.service.GarageBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Dispara a carga da garagem assim que a aplicacao sobe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GarageBootstrapRunner implements ApplicationRunner {

    private final GarageBootstrapService bootstrapService;
    private final SimulatorProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.bootstrapEnabled()) {
            log.info("Bootstrap da garagem desabilitado (simulator.bootstrap-enabled=false).");
            return;
        }
        try {
            bootstrapService.loadGarage();
        } catch (Exception ex) {
            log.error("Falha ao carregar a garagem do simulador em {}. "
                            + "Verifique se o simulador está no ar. Detalhe: {}",
                    properties.baseUrl(), ex.getMessage());
        }
    }
}
