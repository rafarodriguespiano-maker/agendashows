CREATE TABLE show (
    id BIGINT NOT NULL AUTO_INCREMENT,
    data DATE NOT NULL,
    artista VARCHAR(150) NOT NULL,
    local VARCHAR(200) NOT NULL,
    horario TIME NULL,
    observacoes VARCHAR(2000) NULL,
    cache DECIMAL(10, 2) NULL,
    pago BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_show_data ON show (data);
