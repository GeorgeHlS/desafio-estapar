package com.estapar.parking.web.dto;

import java.time.OffsetDateTime;

/**
 * Corpo padronizado de erro da API.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message
) {
}
