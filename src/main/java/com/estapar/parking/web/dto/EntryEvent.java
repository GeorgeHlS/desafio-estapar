package com.estapar.parking.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Evento de entrada do veículo pela cancela.
 */
public record EntryEvent(
        @JsonProperty("license_plate") @NotBlank String licensePlate,
        @JsonProperty("entry_time") @NotNull OffsetDateTime entryTime,
        @JsonProperty("event_type") String eventType
) implements WebhookEvent {
}
