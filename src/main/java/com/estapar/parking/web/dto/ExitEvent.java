package com.estapar.parking.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Evento de saída do veículo.
 */
public record ExitEvent(
        @JsonProperty("license_plate") @NotBlank String licensePlate,
        @JsonProperty("exit_time") @NotNull OffsetDateTime exitTime,
        @JsonProperty("event_type") String eventType
) implements WebhookEvent {
}
