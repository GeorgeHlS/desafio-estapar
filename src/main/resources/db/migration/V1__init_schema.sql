-- ============================================================
--  Schema inicial do sistema de estacionamento
-- ============================================================

-- Setor: divisão lógica do pool de vagas (sector, basePrice, max_capacity)
CREATE TABLE sector (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(50)  NOT NULL,
    base_price    DECIMAL(10, 2) NOT NULL,
    max_capacity  INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sector_name (name)
) ENGINE = InnoDB;

-- Vaga física pertencente a um setor
CREATE TABLE spot (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    external_id   BIGINT       NOT NULL,
    sector_id     BIGINT       NOT NULL,
    lat           DOUBLE       NULL,
    lng           DOUBLE       NULL,
    occupied      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_spot_sector FOREIGN KEY (sector_id) REFERENCES sector (id)
) ENGINE = InnoDB;

CREATE INDEX idx_spot_sector ON spot (sector_id);
CREATE INDEX idx_spot_occupied ON spot (sector_id, occupied);

-- Sessão de estacionamento: ciclo de vida de um veículo (ENTRY -> PARKED -> EXIT)
CREATE TABLE parking_session (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    license_plate      VARCHAR(20) NOT NULL,
    sector_id          BIGINT      NULL,
    spot_id            BIGINT      NULL,
    entry_time         DATETIME(3) NOT NULL,
    parked_time        DATETIME(3) NULL,
    exit_time          DATETIME(3) NULL,
    price_factor       DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
    amount_charged     DECIMAL(10, 2) NULL,
    status             VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_session_sector FOREIGN KEY (sector_id) REFERENCES sector (id),
    CONSTRAINT fk_session_spot   FOREIGN KEY (spot_id)   REFERENCES spot (id)
) ENGINE = InnoDB;

CREATE INDEX idx_session_plate_status ON parking_session (license_plate, status);
CREATE INDEX idx_session_sector ON parking_session (sector_id);
CREATE INDEX idx_session_exit_time ON parking_session (exit_time);
