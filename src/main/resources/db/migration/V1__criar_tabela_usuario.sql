CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome_usuario VARCHAR(50) NOT NULL,
    senha VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_nome_usuario UNIQUE (nome_usuario)
);
