package com.estapar.parking.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resposta da consulta de faturamento.
 */
public record RevenueResponse(
        BigDecimal amount,
        String currency,
        OffsetDateTime timestamp
) {
}
