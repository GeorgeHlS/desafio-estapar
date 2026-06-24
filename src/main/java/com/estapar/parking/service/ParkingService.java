package com.estapar.parking.service;

import com.estapar.parking.domain.ParkingSession;
import com.estapar.parking.domain.SessionStatus;
import com.estapar.parking.domain.Spot;
import com.estapar.parking.exception.BusinessException;
import com.estapar.parking.exception.GarageFullException;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SpotRepository;
import com.estapar.parking.web.dto.EntryEvent;
import com.estapar.parking.web.dto.ExitEvent;
import com.estapar.parking.web.dto.ParkedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Gerencia as sessões de estacionamento e garante que cada evento do webhook
 * seja processado com as regras de negócio corretas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingService {

    private final SpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PricingService pricingService;

    /**
     * Entrada de carro: valida duplicidade de sessão e persiste uma nova sessão
     * com o fator de preço dinâmico calculado no momento da entrada.
     */
    @Transactional
    public void handleEntry(EntryEvent event) {
        // Evita duplicar sessão aberta para a mesma placa.
        sessionRepository
                .findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                        event.licensePlate(), SessionStatus.EXITED)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Placa " + event.licensePlate() + " já possui sessão aberta.");
                });

        long total = spotRepository.count();
        long occupied = spotRepository.findAll().stream().filter(Spot::isOccupied).count();

        if (total > 0 && occupied >= total) {
            throw new GarageFullException(
                    "Estacionamento lotado. Entrada de " + event.licensePlate() + " recusada.");
        }

        BigDecimal factor = pricingService.dynamicPriceFactor(occupied, total);

        ParkingSession session = ParkingSession.builder()
                .licensePlate(event.licensePlate())
                .entryTime(event.entryTime())
                .priceFactor(factor)
                .status(SessionStatus.ENTERED)
                .build();
        sessionRepository.save(session);

        log.info("ENTRY placa={} fatorDinamico={} ocupacao={}/{}",
                event.licensePlate(), factor, occupied, total);
    }

    /**
     * Estacionamento: encontra a vaga a partir das coordenadas e marca o setor
     * como ocupado, validando se o setor ainda tem capacidade.
     */
    @Transactional
    public void handleParked(ParkedEvent event) {
        ParkingSession session = openSessionOrThrow(event.licensePlate());

        Spot spot = spotRepository.findFirstByLatAndLng(event.lat(), event.lng())
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma vaga encontrada para lat=" + event.lat() + " lng=" + event.lng()));

        if (spot.isOccupied()) {
            throw new BusinessException("Vaga (id=" + spot.getId() + ") já está ocupada.");
        }

        Long sectorId = spot.getSector().getId();
        long sectorTotal = spotRepository.countBySectorId(sectorId);
        long sectorOccupied = spotRepository.countBySectorIdAndOccupiedTrue(sectorId);

        // Regra de lotação: com 100% de ocupação o setor está fechado.
        if (sectorTotal > 0 && sectorOccupied >= sectorTotal) {
            throw new GarageFullException(
                    "Setor " + spot.getSector().getName() + " lotado. PARKED recusado.");
        }

        spot.setOccupied(true);
        spotRepository.save(spot);

        session.setSpot(spot);
        session.setSector(spot.getSector());
        session.setParkedTime(session.getEntryTime() != null ? session.getEntryTime().plusMinutes(2) : java.time.OffsetDateTime.now());
        session.setStatus(SessionStatus.PARKED);
        sessionRepository.save(session);

        log.info("PARKED placa={} setor={} vaga={} ocupacaoSetor={}/{}",
                event.licensePlate(), spot.getSector().getName(), spot.getId(),
                sectorOccupied + 1, sectorTotal);
    }

    /**
     * Saída do veículo: libera a vaga ocupada e calcula o valor final com base
     * no tempo de permanência e no fator dinâmico definido na entrada.
     */
    @Transactional
    public void handleExit(ExitEvent event) {
        ParkingSession session = openSessionOrThrow(event.licensePlate());

        BigDecimal amount = BigDecimal.ZERO;
        if (session.getSector() != null) {
            amount = pricingService.calculateAmount(
                    session.getEntryTime(),
                    event.exitTime(),
                    session.getSector().getBasePrice(),
                    session.getPriceFactor());
        } else {
            log.warn("EXIT placa={} sem setor associado (sem PARKED). Cobrando 0.",
                    event.licensePlate());
        }

        // Libera a vaga ocupada, se houver.
        Spot spot = session.getSpot();
        if (spot != null) {
            spot.setOccupied(false);
            spotRepository.save(spot);
        }

        session.setExitTime(event.exitTime());
        session.setAmountCharged(amount);
        session.setStatus(SessionStatus.EXITED);
        sessionRepository.save(session);

        log.info("EXIT placa={} valor={} setor={}",
                event.licensePlate(), amount,
                session.getSector() != null ? session.getSector().getName() : "-");
    }

    private ParkingSession openSessionOrThrow(String licensePlate) {
        return sessionRepository
                .findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                        licensePlate, SessionStatus.EXITED)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma sessão aberta para a placa " + licensePlate + "."));
    }
}
