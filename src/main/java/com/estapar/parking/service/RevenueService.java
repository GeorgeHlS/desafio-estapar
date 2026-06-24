package com.estapar.parking.service;

import com.estapar.parking.domain.Sector;
import com.estapar.parking.exception.BusinessException;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.web.dto.RevenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Calcula o faturamento total de um setor em uma data.
 */
@Service
@RequiredArgsConstructor
public class RevenueService {

    private static final String CURRENCY = "BRL";

    private final SectorRepository sectorRepository;
    private final ParkingSessionRepository sessionRepository;

    /**
     * Soma o valor cobrado das sessões que SAIRAM (EXIT) no dia informado,
     * para o setor informado. O dia é considerado em UTC, alinhado ao formato
     * de timestamps do simulador (..."Z").
     *
     * @param date   data (YYYY-MM-DD)
     * @param sector nome do setor (ex.: "A")
     */
    @Transactional(readOnly = true)
    public RevenueResponse calculateRevenue(LocalDate date, String sector) {
        Sector found = sectorRepository.findByName(sector)
                .orElseThrow(() -> new BusinessException("Setor '" + sector + "' não encontrado."));

        OffsetDateTime start = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = start.plusDays(1);

        BigDecimal amount = sessionRepository
                .sumRevenueBySectorAndPeriod(found.getId(), start, end);
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        return new RevenueResponse(amount, CURRENCY, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
