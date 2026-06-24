package com.estapar.parking.exception;

/**
 * Lançada quando o setor está 100% lotado e não pode aceitar novas entradas.
 */
public class GarageFullException extends RuntimeException {
    public GarageFullException(String message) {
        super(message);
    }
}
