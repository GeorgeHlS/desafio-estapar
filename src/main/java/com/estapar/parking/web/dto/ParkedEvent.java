package com.estapar.parking.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Evento de estacionamento em uma vaga.
 */
public record ParkedEvent(
        @JsonProperty("license_plate") @NotBlank String licensePlate,
        @JsonProperty("lat") @NotNull Double lat,
        @JsonProperty("lng") @NotNull Double lng,
        @JsonProperty("event_type") String eventType
) implements WebhookEvent {
}
