package com.estapar.parking.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GarageConfigResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeGarageConfigWithBasePriceCamelCase() throws Exception {
        String json = "{\"garage\":[{\"sector\":\"A\",\"basePrice\":10.0,\"max_capacity\":100}],\"spots\":[]}";

        GarageConfigResponse response = objectMapper.readValue(json, GarageConfigResponse.class);

        assertThat(response.garage()).hasSize(1);
        assertThat(response.garage().get(0).basePrice()).isEqualByComparingTo(new BigDecimal("10.0"));
    }

    @Test
    void shouldDeserializeGarageConfigWithBasePriceSnakeCase() throws Exception {
        String json = "{\"garage\":[{\"sector\":\"A\",\"base_price\":10.0,\"max_capacity\":100}],\"spots\":[]}";

        GarageConfigResponse response = objectMapper.readValue(json, GarageConfigResponse.class);

        assertThat(response.garage()).hasSize(1);
        assertThat(response.garage().get(0).basePrice()).isEqualByComparingTo(new BigDecimal("10.0"));
    }
}
