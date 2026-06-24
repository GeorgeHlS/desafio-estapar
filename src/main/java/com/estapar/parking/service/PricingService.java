package com.estapar.parking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Regras de precificação do estacionamento, incluindo janela gratuita e ajuste
 * de tarifa com base na ocupação do setor.
 */
@Service
public class PricingService {

    /** Janela de carencia gratuita, em minutos. */
    static final long FREE_MINUTES = 30;

    /**
     * Retorna o multiplicador de preço usado no momento da entrada, conforme a
     * taxa de ocupação do setor.
     * @param occupiedSpots vagas ocupadas no setor
     * @param totalSpots    capacidade total do setor (deve ser &gt; 0)
     * @return multiplicador aplicado sobre o preço base do setor
     */
    public BigDecimal dynamicPriceFactor(long occupiedSpots, long totalSpots) {
        if (totalSpots <= 0) {
            return BigDecimal.ONE;
        }
        double occupancyRate = (double) occupiedSpots / (double) totalSpots;

        if (occupancyRate < 0.25) {
            return new BigDecimal("0.90");
        } else if (occupancyRate < 0.50) {
            return new BigDecimal("1.00");
        } else if (occupancyRate < 0.75) {
            return new BigDecimal("1.10");
        } else {
            return new BigDecimal("1.25");
        }
    }

    /**
     * Calcula a cobrança final considerando os primeiros 30 minutos gratuitos,
     * a cobrança por hora cheia e o multiplicador de preço definido na entrada.
     * @param entryTime   horário de entrada
     * @param exitTime    horário de saída
     * @param basePrice   preço base por hora do setor
     * @param priceFactor multiplicador dinâmico capturado na entrada
     * @return valor a cobrar (escala 2, arredondamento HALF_UP no valor final)
     */
    public BigDecimal calculateAmount(OffsetDateTime entryTime,
                                      OffsetDateTime exitTime,
                                      BigDecimal basePrice,
                                      BigDecimal priceFactor) {
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("exitTime nao pode ser anterior a entryTime");
        }

        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();

        // Primeiros 30 minutos sao gratis.
        if (totalMinutes <= FREE_MINUTES) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Cobra por hora cheia, arredondando para cima
        long billableHours = (long) Math.ceil(totalMinutes / 60.0);
        if (billableHours < 1) {
            billableHours = 1;
        }

        BigDecimal hourlyPrice = basePrice.multiply(priceFactor);
        return hourlyPrice
                .multiply(BigDecimal.valueOf(billableHours))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
