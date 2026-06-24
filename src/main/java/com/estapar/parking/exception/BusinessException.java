package com.estapar.parking.exception;

/**
 * Erro de regra de negócio recuperável
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
