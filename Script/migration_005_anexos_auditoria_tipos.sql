USE conecta21;

ALTER TABLE usuarios
  ADD COLUMN excluido BIT NOT NULL DEFAULT b'0',
  ADD COLUMN avatar_nome_original VARCHAR(255) NULL,
  ADD COLUMN avatar_tipo_mime VARCHAR(120) NULL,
  ADD COLUMN avatar_caminho_relativo VARCHAR(500) NULL;

ALTER TABLE chamados
  ADD COLUMN tipo VARCHAR(30) NOT NULL DEFAULT 'SUPORTE_EXTERNO',
  ADD COLUMN excluido BIT NOT NULL DEFAULT b'0';

ALTER TABLE artigos_faq
  ADD COLUMN excluido BIT NOT NULL DEFAULT b'0';

UPDATE artigos_faq SET data_criacao = CURRENT_TIMESTAMP WHERE data_criacao IS NULL;
ALTER TABLE artigos_faq MODIFY COLUMN data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE anexos_chamados (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  chamado_id BIGINT NOT NULL,
  nome_original VARCHAR(255) NOT NULL,
  nome_armazenado VARCHAR(255) NOT NULL UNIQUE,
  tipo_mime VARCHAR(120) NULL,
  tamanho_bytes BIGINT NOT NULL,
  caminho_relativo VARCHAR(500) NOT NULL,
  data_upload DATETIME NOT NULL,
  CONSTRAINT fk_anexo_chamado FOREIGN KEY (chamado_id) REFERENCES chamados(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE logs_auditoria (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  empresa_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  nome_usuario VARCHAR(255) NOT NULL,
  acao VARCHAR(255) NOT NULL,
  entidade VARCHAR(255) NOT NULL,
  entidade_id BIGINT NOT NULL,
  detalhes TEXT NULL,
  data_criacao DATETIME NOT NULL,
  KEY idx_auditoria_entidade (empresa_id, entidade, entidade_id, data_criacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
