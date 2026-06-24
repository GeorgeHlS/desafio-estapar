package com.estapar.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Sessão de estacionamento de um veículo: representa o ciclo ENTRY -> PARKED -> EXIT para uma placa
 */
@Entity
@Table(name = "parking_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    @Column(name = "entry_time", nullable = false)
    private OffsetDateTime entryTime;

    @Column(name = "parked_time")
    private OffsetDateTime parkedTime;

    @Column(name = "exit_time")
    private OffsetDateTime exitTime;

    @Column(name = "price_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal priceFactor;

    @Column(name = "amount_charged", precision = 10, scale = 2)
    private BigDecimal amountCharged;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;
}
