package com.estapar.parking.service;

import com.estapar.parking.client.GarageSimulatorClient;
import com.estapar.parking.client.dto.GarageConfigResponse;
import com.estapar.parking.domain.Sector;
import com.estapar.parking.domain.Spot;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.SpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GarageBootstrapServiceTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private SpotRepository spotRepository;

    private static class FakeGarageSimulatorClient extends GarageSimulatorClient {
        private final GarageConfigResponse response;
        private final boolean shouldThrow;

        FakeGarageSimulatorClient(GarageConfigResponse response, boolean shouldThrow) {
            super(null);
            this.response = response;
            this.shouldThrow = shouldThrow;
        }

        @Override
        public GarageConfigResponse fetchGarageConfig() {
            if (shouldThrow) {
                throw new AssertionError("fetchGarageConfig should not be called");
            }
            return response;
        }
    }

    private GarageBootstrapService bootstrapService(GarageConfigResponse response) {
        return new GarageBootstrapService(new FakeGarageSimulatorClient(response, false), sectorRepository, spotRepository);
    }

    private GarageBootstrapService bootstrapServiceNoFetch() {
        return new GarageBootstrapService(new FakeGarageSimulatorClient(null, true), sectorRepository, spotRepository);
    }

    @Test
    void loadGarage_whenSectorsExist_doesNotFetchSimulator() {
        given(sectorRepository.count()).willReturn(1L);

        bootstrapServiceNoFetch().loadGarage();

        verify(spotRepository, never()).save(any(Spot.class));
    }

    @Test
    void loadGarage_whenSimulatorReturnsEmptyGarage_doesNotSaveAnything() {
        given(sectorRepository.count()).willReturn(0L);
        GarageConfigResponse response = new GarageConfigResponse(List.of(), List.of());

        bootstrapService(response).loadGarage();

        verify(sectorRepository, never()).save(any(Sector.class));
        verify(spotRepository, never()).save(any(Spot.class));
    }

    @Test
    void loadGarage_savesSectorsAndSpotsFromSimulatorConfig() {
        given(sectorRepository.count()).willReturn(0L);
        GarageConfigResponse.SectorConfig sectorConfig = new GarageConfigResponse.SectorConfig("A", new BigDecimal("10.00"), 2);
        GarageConfigResponse.SpotConfig spotConfig = new GarageConfigResponse.SpotConfig(1L, "A", -23.561684, -46.655981);
        GarageConfigResponse config = new GarageConfigResponse(List.of(sectorConfig), List.of(spotConfig));
        given(sectorRepository.save(any(Sector.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(spotRepository.save(any(Spot.class))).willAnswer(invocation -> invocation.getArgument(0));

        bootstrapService(config).loadGarage();

        verify(sectorRepository).save(any(Sector.class));
        verify(spotRepository).save(any(Spot.class));
    }

    @Test
    void loadGarage_ignoresSpotWithMissingSector() {
        given(sectorRepository.count()).willReturn(0L);
        GarageConfigResponse.SectorConfig sectorConfig = new GarageConfigResponse.SectorConfig("A", new BigDecimal("10.00"), 2);
        GarageConfigResponse.SpotConfig spotConfig = new GarageConfigResponse.SpotConfig(1L, "B", -23.561684, -46.655981);
        GarageConfigResponse config = new GarageConfigResponse(List.of(sectorConfig), List.of(spotConfig));
        given(sectorRepository.save(any(Sector.class))).willAnswer(invocation -> invocation.getArgument(0));

        bootstrapService(config).loadGarage();

        verify(sectorRepository).save(any(Sector.class));
        verify(spotRepository, never()).save(any(Spot.class));
    }
}
