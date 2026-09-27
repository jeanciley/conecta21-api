-- Execute uma vez em bancos existentes para habilitar anexos de imagem nas interações.
CREATE TABLE anexos_interacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interacao_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    tamanho BIGINT NOT NULL,
    conteudo MEDIUMBLOB NOT NULL,
    CONSTRAINT fk_anexo_interacao FOREIGN KEY (interacao_id)
        REFERENCES interacoes_chamado(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
