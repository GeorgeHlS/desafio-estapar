package com.estapar.parking.repository;

import com.estapar.parking.domain.Spot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpotRepository extends JpaRepository<Spot, Long> {

    long countBySectorIdAndOccupiedTrue(Long sectorId);

    long countBySectorId(Long sectorId);

    Optional<Spot> findByExternalIdAndSectorId(Long externalId, Long sectorId);

    /**
     * Localiza a vaga (mais próxima) pelas coordenadas reportadas no evento PARKED.
     */
    Optional<Spot> findFirstByLatAndLng(Double lat, Double lng);

    /**
     * Busca uma vaga livre do setor com lock pessimista, evitando que duas entradas concorrentes ocupem a mesma vaga
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Spot s
            where s.sector.id = :sectorId and s.occupied = false
            order by s.id asc
            """)
    Optional<Spot> findFirstFreeSpotForUpdate(@Param("sectorId") Long sectorId);
}
