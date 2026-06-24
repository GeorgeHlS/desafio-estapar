package com.estapar.parking.repository;

import com.estapar.parking.domain.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByName(String name);

    boolean existsByName(String name);
}
