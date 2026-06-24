package com.estapar.parking.web.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Evento recebido no webhook. O tipo concreto e resolvido pelo campo
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "event_type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EntryEvent.class, name = "ENTRY"),
        @JsonSubTypes.Type(value = ParkedEvent.class, name = "PARKED"),
        @JsonSubTypes.Type(value = ExitEvent.class, name = "EXIT")
})
public sealed interface WebhookEvent
        permits EntryEvent, ParkedEvent, ExitEvent {

    String eventType();
}
