package com.estapar.parking.web;

import com.estapar.parking.exception.BusinessException;
import com.estapar.parking.exception.GarageFullException;
import com.estapar.parking.web.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Converte exceções de domínio e de validação em respostas HTTP com status
 * mapeado e corpo de erro consistente.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Retorna 409 quando a tentativa de estacionar ocorre em setor lotado. */
    @ExceptionHandler(GarageFullException.class)
    public ResponseEntity<ApiError> handleGarageFull(GarageFullException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Retorna 422 para erros de negócio que não podem ser processados. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /** Payload inválido / não desserializável -> 400. */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Catch-all -> 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Erro inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno.");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        ApiError body = new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message);
        return ResponseEntity.status(status).body(body);
    }
}
