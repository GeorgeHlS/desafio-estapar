package com.estapar.parking.service;

import com.estapar.parking.domain.ParkingSession;
import com.estapar.parking.domain.Sector;
import com.estapar.parking.domain.SessionStatus;
import com.estapar.parking.domain.Spot;
import com.estapar.parking.exception.BusinessException;
import com.estapar.parking.exception.GarageFullException;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParkingServiceUnitTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private ParkingSessionRepository sessionRepository;

    private final PricingService pricingService = new PricingService();

    private ParkingService parkingService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        parkingService = new ParkingService(spotRepository, sessionRepository, pricingService);
    }

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.of(2025, 1, 1, hour, minute, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void handleEntry_whenOpenSessionExists_throwsBusinessException() {
        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "AAA1111", SessionStatus.EXITED))
                .willReturn(Optional.of(ParkingSession.builder()
                        .licensePlate("AAA1111")
                        .status(SessionStatus.ENTERED)
                        .build()));

        assertThatThrownBy(() -> parkingService.handleEntry(
                        new com.estapar.parking.web.dto.EntryEvent("AAA1111", at(10, 0), "ENTRY")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já possui sessão aberta");

        verify(sessionRepository, never()).save(any(ParkingSession.class));
    }

    @Test
    void handleEntry_whenGarageFull_throwsGarageFullException() {
        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "BBB2222", SessionStatus.EXITED))
                .willReturn(Optional.empty());
        given(spotRepository.count()).willReturn(1L);
        given(spotRepository.countByOccupiedTrue()).willReturn(1L);

        assertThatThrownBy(() -> parkingService.handleEntry(
                        new com.estapar.parking.web.dto.EntryEvent("BBB2222", at(10, 0), "ENTRY")))
                .isInstanceOf(GarageFullException.class)
                .hasMessageContaining("lotado");

        verify(sessionRepository, never()).save(any(ParkingSession.class));
    }

    @Test
    void handleEntry_whenNoOpenSession_savesEnteredSession() {
        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "CCC3333", SessionStatus.EXITED))
                .willReturn(Optional.empty());
        given(spotRepository.count()).willReturn(2L);
        given(spotRepository.countByOccupiedTrue()).willReturn(1L);
        given(sessionRepository.save(any(ParkingSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        parkingService.handleEntry(new com.estapar.parking.web.dto.EntryEvent(
                "CCC3333", at(10, 0), "ENTRY"));

        ArgumentCaptor<ParkingSession> captor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepository).save(captor.capture());

        ParkingSession saved = captor.getValue();
        assertThat(saved.getLicensePlate()).isEqualTo("CCC3333");
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.ENTERED);
        assertThat(saved.getPriceFactor()).isEqualByComparingTo(pricingService.dynamicPriceFactor(1, 2));
        assertThat(saved.getEntryTime()).isEqualTo(at(10, 0));
    }

    @Test
    void handleParked_withoutOpenSession_throwsBusinessException() {
        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "DDD4444", SessionStatus.EXITED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> parkingService.handleParked(
                        new com.estapar.parking.web.dto.ParkedEvent("DDD4444", -23.0, -46.0, "PARKED")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nenhuma sessão aberta");

        verify(spotRepository, never()).findFirstByLatAndLng(any(), any());
    }

    @Test
    void handleParked_whenSpotNotFound_throwsBusinessException() {
        ParkingSession session = ParkingSession.builder()
                .licensePlate("EEE5555")
                .status(SessionStatus.ENTERED)
                .build();
        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "EEE5555", SessionStatus.EXITED))
                .willReturn(Optional.of(session));
        given(spotRepository.findFirstByLatAndLng(-23.0, -46.0)).willReturn(Optional.empty());

        assertThatThrownBy(() -> parkingService.handleParked(
                        new com.estapar.parking.web.dto.ParkedEvent("EEE5555", -23.0, -46.0, "PARKED")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nenhuma vaga encontrada");

        verify(sessionRepository, never()).save(any(ParkingSession.class));
    }

    @Test
    void handleParked_whenSpotAlreadyOccupied_throwsBusinessException() {
        ParkingSession session = ParkingSession.builder()
                .licensePlate("FFF6666")
                .status(SessionStatus.ENTERED)
                .build();
        Spot spot = Spot.builder()
                .occupied(true)
                .build();

        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "FFF6666", SessionStatus.EXITED))
                .willReturn(Optional.of(session));
        given(spotRepository.findFirstByLatAndLng(1.0, 2.0)).willReturn(Optional.of(spot));

        assertThatThrownBy(() -> parkingService.handleParked(
                        new com.estapar.parking.web.dto.ParkedEvent("FFF6666", 1.0, 2.0, "PARKED")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está ocupada");

        verify(sessionRepository, never()).save(any(ParkingSession.class));
    }

    @Test
    void handleParked_whenSectorFull_throwsGarageFullException() {
        Sector sector = Sector.builder().id(1L).name("A").build();
        ParkingSession session = ParkingSession.builder()
                .licensePlate("GGG7777")
                .status(SessionStatus.ENTERED)
                .build();
        Spot spot = Spot.builder()
                .sector(sector)
                .occupied(false)
                .build();

        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "GGG7777", SessionStatus.EXITED))
                .willReturn(Optional.of(session));
        given(spotRepository.findFirstByLatAndLng(1.0, 2.0)).willReturn(Optional.of(spot));
        given(spotRepository.countBySectorId(1L)).willReturn(1L);
        given(spotRepository.countBySectorIdAndOccupiedTrue(1L)).willReturn(1L);

        assertThatThrownBy(() -> parkingService.handleParked(
                        new com.estapar.parking.web.dto.ParkedEvent("GGG7777", 1.0, 2.0, "PARKED")))
                .isInstanceOf(GarageFullException.class)
                .hasMessageContaining("lotado");

        verify(sessionRepository, never()).save(any(ParkingSession.class));
    }

    @Test
    void handleParked_whenSpotAvailable_marksSpotOccupiedAndUpdatesSession() {
        Sector sector = Sector.builder().id(2L).name("B").basePrice(new BigDecimal("10.00")).build();
        Spot spot = Spot.builder()
                .id(10L)
                .sector(sector)
                .lat(-23.0)
                .lng(-46.0)
                .occupied(false)
                .build();
        ParkingSession session = ParkingSession.builder()
                .licensePlate("HHH8888")
                .status(SessionStatus.ENTERED)
                .entryTime(at(10, 0))
                .build();

        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "HHH8888", SessionStatus.EXITED))
                .willReturn(Optional.of(session));
        given(spotRepository.findFirstByLatAndLng(-23.0, -46.0)).willReturn(Optional.of(spot));
        given(spotRepository.countBySectorId(2L)).willReturn(2L);
        given(spotRepository.countBySectorIdAndOccupiedTrue(2L)).willReturn(0L);
        given(sessionRepository.save(any(ParkingSession.class))).willAnswer(invocation -> invocation.getArgument(0));

        parkingService.handleParked(new com.estapar.parking.web.dto.ParkedEvent("HHH8888", -23.0, -46.0, "PARKED"));

        assertThat(spot.isOccupied()).isTrue();
        ArgumentCaptor<ParkingSession> captor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepository).save(captor.capture());

        ParkingSession saved = captor.getValue();
        assertThat(saved.getSpot()).isEqualTo(spot);
        assertThat(saved.getSector()).isEqualTo(sector);
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.PARKED);
        assertThat(saved.getParkedTime()).isNotNull();
    }

    @Test
    void handleExit_withoutOpenSession_throwsBusinessException() {
        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "III9999", SessionStatus.EXITED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> parkingService.handleExit(
                        new com.estapar.parking.web.dto.ExitEvent("III9999", at(12, 0), "EXIT")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nenhuma sessão aberta");
    }

    @Test
    void handleExit_withoutSector_setsZeroAmountAndExitStatus() {
        ParkingSession session = ParkingSession.builder()
                .licensePlate("JJJ0000")
                .status(SessionStatus.ENTERED)
                .spot(null)
                .sector(null)
                .priceFactor(new BigDecimal("1.00"))
                .entryTime(at(10, 0))
                .build();
        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "JJJ0000", SessionStatus.EXITED))
                .willReturn(Optional.of(session));
        given(sessionRepository.save(any(ParkingSession.class))).willAnswer(invocation -> invocation.getArgument(0));

        parkingService.handleExit(new com.estapar.parking.web.dto.ExitEvent("JJJ0000", at(11, 0), "EXIT"));

        ArgumentCaptor<ParkingSession> captor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepository).save(captor.capture());

        ParkingSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.EXITED);
        assertThat(saved.getAmountCharged()).isEqualByComparingTo("0.00");
    }

    @Test
    void handleExit_withParkingSession_calculatesAmountAndFreesSpot() {
        Sector sector = Sector.builder().id(3L).name("C").basePrice(new BigDecimal("10.00")).build();
        Spot spot = Spot.builder().id(20L).occupied(true).build();
        ParkingSession session = ParkingSession.builder()
                .licensePlate("KKK1111")
                .status(SessionStatus.PARKED)
                .sector(sector)
                .spot(spot)
                .priceFactor(new BigDecimal("1.25"))
                .entryTime(at(10, 0))
                .build();

        given(sessionRepository.findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
                "KKK1111", SessionStatus.EXITED))
                .willReturn(Optional.of(session));
        given(sessionRepository.save(any(ParkingSession.class))).willAnswer(invocation -> invocation.getArgument(0));

        parkingService.handleExit(new com.estapar.parking.web.dto.ExitEvent("KKK1111", at(12, 0), "EXIT"));

        assertThat(spot.isOccupied()).isFalse();
        verify(spotRepository).save(spot);

        ArgumentCaptor<ParkingSession> captor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepository).save(captor.capture());

        ParkingSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.EXITED);
        assertThat(saved.getAmountCharged()).isEqualByComparingTo("25.00");
        assertThat(saved.getExitTime()).isEqualTo(at(12, 0));
    }
}
