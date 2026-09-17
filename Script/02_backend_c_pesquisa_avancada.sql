-- ETAPA 2 - Backend C: Pesquisa Avançada
-- Requer MySQL 8+ e deve ser executado uma única vez após a etapa 1.
-- Os índices FULLTEXT são usados pelas consultas MATCH(...) AGAINST(...).

CREATE TABLE IF NOT EXISTS artigos_faq (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    conteudo TEXT NOT NULL,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_artigo_empresa (empresa_id),
    KEY idx_artigo_autor (autor_id),
    CONSTRAINT fk_artigo_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT fk_artigo_autor
        FOREIGN KEY (autor_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE chamados
    ADD FULLTEXT INDEX ft_chamados_titulo_descricao (titulo, descricao);

ALTER TABLE artigos_faq
    ADD FULLTEXT INDEX ft_artigos_titulo_conteudo (titulo, conteudo);
