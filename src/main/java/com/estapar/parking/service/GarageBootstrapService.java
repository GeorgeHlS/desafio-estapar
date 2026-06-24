package com.estapar.parking.service;

import com.estapar.parking.client.GarageSimulatorClient;
import com.estapar.parking.client.dto.GarageConfigResponse;
import com.estapar.parking.domain.Sector;
import com.estapar.parking.domain.Spot;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Inicializa a garagem com os dados do simulador apenas quando o banco ainda
 * não possui setores cadastrados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GarageBootstrapService {

    private final GarageSimulatorClient simulatorClient;
    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;

    /**
     * Busca a configuração no simulador e popula setores e vagas se a garagem
     * ainda não tiver sido inicializada no banco.
     */
    @Transactional
    public void loadGarage() {
        if (sectorRepository.count() > 0) {
            log.info("Garagem já inicializada ({} setores). Pulando carga.",
                    sectorRepository.count());
            return;
        }

        GarageConfigResponse config = simulatorClient.fetchGarageConfig();
        if (config == null || config.garage() == null || config.garage().isEmpty()) {
            log.warn("Simulador retornou configuracao de garagem vazia.");
            return;
        }

        Map<String, Sector> sectorsByName = new HashMap<>();
        for (GarageConfigResponse.SectorConfig sc : config.garage()) {
            Sector sector = Sector.builder()
                    .name(sc.sector())
                    .basePrice(sc.basePrice())
                    .maxCapacity(sc.maxCapacity() != null ? sc.maxCapacity() : 0)
                    .build();
            sector = sectorRepository.save(sector);
            sectorsByName.put(sc.sector(), sector);
        }

        int spotCount = 0;
        if (config.spots() != null) {
            for (GarageConfigResponse.SpotConfig sp : config.spots()) {
                Sector sector = sectorsByName.get(sp.sector());
                if (sector == null) {
                    log.warn("Vaga {} referencia setor inexistente '{}'. Ignorando.",
                            sp.id(), sp.sector());
                    continue;
                }
                Spot spot = Spot.builder()
                        .externalId(sp.id())
                        .sector(sector)
                        .lat(sp.lat())
                        .lng(sp.lng())
                        .occupied(false)
                        .build();
                spotRepository.save(spot);
                spotCount++;
            }
        }

        log.info("Garagem carregada: {} setores, {} vagas.",
                sectorsByName.size(), spotCount);
    }
}
