package com.estapar.parking.web;

import com.estapar.parking.service.ParkingService;
import com.estapar.parking.web.dto.EntryEvent;
import com.estapar.parking.web.dto.ExitEvent;
import com.estapar.parking.web.dto.ParkedEvent;
import com.estapar.parking.web.dto.WebhookEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe os eventos do simulador e encaminha para o serviço de estacionamento.
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final ParkingService parkingService;

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody WebhookEvent event) {
        switch (event) {
            case EntryEvent e -> parkingService.handleEntry(e);
            case ParkedEvent e -> parkingService.handleParked(e);
            case ExitEvent e -> parkingService.handleExit(e);
        }
        // Todos os eventos respondem HTTP 200 conforme o contrato.
        return ResponseEntity.ok().build();
    }
}
