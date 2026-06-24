package com.estapar.parking.repository;

import com.estapar.parking.domain.ParkingSession;
import com.estapar.parking.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    /**
     * Sessão aberta (ainda não finalizada) de uma placa.
     * Uma placa não deveria ter duas sessões simultâneas em aberto.
     */
    Optional<ParkingSession> findFirstByLicensePlateAndStatusNotOrderByEntryTimeDesc(
            String licensePlate, SessionStatus status);

    /**
     * Soma o faturamento de um setor em uma data (baseado no horário de saída).
     * Retorna a soma de amount_charged para sessões com EXIT no intervalo informado.
     */
    @Query("""
            select coalesce(sum(s.amountCharged), 0)
            from ParkingSession s
            where s.sector.id = :sectorId
              and s.status = com.estapar.parking.domain.SessionStatus.EXITED
              and s.exitTime >= :start
              and s.exitTime < :end
            """)
    BigDecimal sumRevenueBySectorAndPeriod(@Param("sectorId") Long sectorId,
                                           @Param("start") OffsetDateTime start,
                                           @Param("end") OffsetDateTime end);

    List<ParkingSession> findByLicensePlateOrderByEntryTimeDesc(String licensePlate);
}
