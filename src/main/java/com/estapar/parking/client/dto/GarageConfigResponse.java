package com.estapar.parking.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta do endpoint GET /garage do simulador
 */
public record GarageConfigResponse(
        List<SectorConfig> garage,
        List<SpotConfig> spots
) {

    public record SectorConfig(
            String sector,
            @JsonAlias({"base_price", "basePrice"}) BigDecimal basePrice,
            @JsonProperty("max_capacity") Integer maxCapacity
    ) {
    }

    public record SpotConfig(
            Long id,
            String sector,
            Double lat,
            Double lng
    ) {
    }
}
