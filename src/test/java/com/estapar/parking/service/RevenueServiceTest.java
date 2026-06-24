package com.estapar.parking.service;

import com.estapar.parking.domain.Sector;
import com.estapar.parking.exception.BusinessException;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.web.dto.RevenueResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParkingSessionRepository sessionRepository;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void calculateRevenue_whenSectorNotFound_throwsBusinessException() {
        given(sectorRepository.findByName("Z")).willReturn(Optional.empty());

        assertThatThrownBy(() -> revenueService.calculateRevenue(LocalDate.of(2025, 1, 1), "Z"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void calculateRevenue_whenNoRevenue_returnsZeroAmount() {
        Sector sector = Sector.builder()
                .id(5L)
                .name("A")
                .basePrice(new BigDecimal("10.00"))
                .maxCapacity(1)
                .build();
        given(sectorRepository.findByName("A")).willReturn(Optional.of(sector));
        given(sessionRepository.sumRevenueBySectorAndPeriod(
                5L,
                OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC)))
                .willReturn(null);

        RevenueResponse response = revenueService.calculateRevenue(LocalDate.of(2025, 1, 1), "A");

        assertThat(response.amount()).isEqualByComparingTo("0.00");
        assertThat(response.currency()).isEqualTo("BRL");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void calculateRevenue_returnsSumAndCurrency() {
        Sector sector = Sector.builder()
                .id(6L)
                .name("B")
                .basePrice(new BigDecimal("12.00"))
                .maxCapacity(10)
                .build();
        given(sectorRepository.findByName("B")).willReturn(Optional.of(sector));
        given(sessionRepository.sumRevenueBySectorAndPeriod(
                6L,
                OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC)))
                .willReturn(new BigDecimal("123.45"));

        RevenueResponse response = revenueService.calculateRevenue(LocalDate.of(2025, 1, 2), "B");

        assertThat(response.amount()).isEqualByComparingTo("123.45");
        assertThat(response.currency()).isEqualTo("BRL");
        assertThat(response.timestamp()).isNotNull();
    }
}
