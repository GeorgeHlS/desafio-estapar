package com.estapar.parking.web;

import com.estapar.parking.service.ParkingService;
import com.estapar.parking.web.dto.EntryEvent;
import com.estapar.parking.web.dto.ExitEvent;
import com.estapar.parking.web.dto.ParkedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookControllerTest {

    private final StubParkingService parkingService = new StubParkingService();
    private final WebhookController controller = new WebhookController(parkingService);

    private static class StubParkingService extends ParkingService {
        private EntryEvent lastEntry;
        private ParkedEvent lastParked;
        private ExitEvent lastExit;

        StubParkingService() {
            super(null, null, null);
        }

        @Override
        public void handleEntry(EntryEvent event) {
            this.lastEntry = event;
        }

        @Override
        public void handleParked(ParkedEvent event) {
            this.lastParked = event;
        }

        @Override
        public void handleExit(ExitEvent event) {
            this.lastExit = event;
        }
    }

    @Test
    void receive_entryEvent_callsHandleEntryAndReturnsOk() {
        EntryEvent event = new EntryEvent("ZUL0001", OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC), "ENTRY");

        ResponseEntity<Void> response = controller.receive(event);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(parkingService.lastEntry).isEqualTo(event);
    }

    @Test
    void receive_parkedEvent_callsHandleParkedAndReturnsOk() {
        ParkedEvent event = new ParkedEvent("ZUL0001", -23.561684, -46.655981, "PARKED");

        ResponseEntity<Void> response = controller.receive(event);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(parkingService.lastParked).isEqualTo(event);
    }

    @Test
    void receive_exitEvent_callsHandleExitAndReturnsOk() {
        ExitEvent event = new ExitEvent("ZUL0001", OffsetDateTime.of(2025, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC), "EXIT");

        ResponseEntity<Void> response = controller.receive(event);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(parkingService.lastExit).isEqualTo(event);
    }
}
