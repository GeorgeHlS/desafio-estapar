package com.estapar.parking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários das regras de precificação.
 */
class PricingServiceTest {

    private final PricingService pricing = new PricingService();
    private final BigDecimal basePrice = new BigDecimal("10.00");

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.of(2025, 1, 1, hour, minute, 0, 0, ZoneOffset.UTC);
    }

    // ---------- Preço dinâmico ----------

    @Test
    @DisplayName("ocupação < 25% -> desconto de 10% (fator 0.90)")
    void dynamicFactorBelow25() {
        // 24 de 100 -> 24%
        assertThat(pricing.dynamicPriceFactor(24, 100)).isEqualByComparingTo("0.90");
    }

    @Test
    @DisplayName("ocupação entre 25% e 50% -> fator 1.00")
    void dynamicFactorBelow50() {
        assertThat(pricing.dynamicPriceFactor(25, 100)).isEqualByComparingTo("1.00");
        assertThat(pricing.dynamicPriceFactor(49, 100)).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("ocupação entre 50% e 75% -> aumento de 10% (fator 1.10)")
    void dynamicFactorBelow75() {
        assertThat(pricing.dynamicPriceFactor(50, 100)).isEqualByComparingTo("1.10");
        assertThat(pricing.dynamicPriceFactor(74, 100)).isEqualByComparingTo("1.10");
    }

    @Test
    @DisplayName("ocupação >= 75% -> aumento de 25% (fator 1.25)")
    void dynamicFactorAbove75() {
        assertThat(pricing.dynamicPriceFactor(75, 100)).isEqualByComparingTo("1.25");
        assertThat(pricing.dynamicPriceFactor(100, 100)).isEqualByComparingTo("1.25");
    }

    @Test
    @DisplayName("setor sem vagas -> fator neutro 1.00")
    void dynamicFactorZeroCapacity() {
        assertThat(pricing.dynamicPriceFactor(0, 0)).isEqualByComparingTo("1.00");
    }

    // ---------- Cálculo de tarifa ----------

    @Test
    @DisplayName("até 30 minutos é grátis")
    void freeUnder30Minutes() {
        BigDecimal amount = pricing.calculateAmount(at(12, 0), at(12, 30), basePrice, BigDecimal.ONE);
        assertThat(amount).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("31 minutos cobra 1 hora cheia")
    void chargesFirstHourAfterGrace() {
        BigDecimal amount = pricing.calculateAmount(at(12, 0), at(12, 31), basePrice, BigDecimal.ONE);
        assertThat(amount).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("1h01 arredonda para 2 horas")
    void roundsUpToNextHour() {
        BigDecimal amount = pricing.calculateAmount(at(12, 0), at(13, 1), basePrice, BigDecimal.ONE);
        assertThat(amount).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("exatamente 1 hora cobra 1 hora")
    void exactlyOneHour() {
        BigDecimal amount = pricing.calculateAmount(at(12, 0), at(13, 0), basePrice, BigDecimal.ONE);
        assertThat(amount).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("aplica fator dinâmico de desconto (0.90) sobre a tarifa")
    void appliesDiscountFactor() {
        BigDecimal amount = pricing.calculateAmount(
                at(12, 0), at(13, 0), basePrice, new BigDecimal("0.90"));
        assertThat(amount).isEqualByComparingTo("9.00");
    }

    @Test
    @DisplayName("aplica fator dinâmico de aumento (1.25) sobre 2 horas")
    void appliesSurgeFactor() {
        BigDecimal amount = pricing.calculateAmount(
                at(12, 0), at(14, 0), basePrice, new BigDecimal("1.25"));
        // 2h * 10.00 * 1.25 = 25.00
        assertThat(amount).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("saida antes da entrada lanca erro")
    void exitBeforeEntryThrows() {
        assertThatThrownBy(() ->
                pricing.calculateAmount(at(13, 0), at(12, 0), basePrice, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
