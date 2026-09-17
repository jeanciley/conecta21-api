-- ETAPA 1 - Backend B: Comunicação e Arquivos
-- Executar em uma base Conecta21 já criada antes de iniciar a aplicação.
-- O projeto usa spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE IF NOT EXISTS anexos_chamados (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chamado_id BIGINT NOT NULL,
    nome_original VARCHAR(255) NOT NULL,
    nome_armazenado VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(120) NULL,
    tamanho_bytes BIGINT NOT NULL,
    caminho_relativo VARCHAR(500) NOT NULL,
    data_upload DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_anexo_nome_armazenado (nome_armazenado),
    KEY idx_anexo_chamado (chamado_id),
    CONSTRAINT fk_anexo_chamado
        FOREIGN KEY (chamado_id) REFERENCES chamados(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
