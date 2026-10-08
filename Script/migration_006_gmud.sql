USE conecta21;

ALTER TABLE empresas
  ADD COLUMN gmud_ativo BIT NOT NULL DEFAULT b'0';

CREATE TABLE gmuds (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  empresa_id BIGINT NOT NULL,
  ticket_id BIGINT NOT NULL,
  responsavel_id BIGINT NOT NULL,
  modelo_utilizado VARCHAR(100) NOT NULL,
  ambiente VARCHAR(40) NOT NULL,
  data_agendada DATETIME NOT NULL,
  riscos_impactos TEXT NOT NULL,
  status_aprovacao VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
  caminho_arquivo_gerado VARCHAR(500) NULL,
  data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_gmud_empresa_ticket (empresa_id, ticket_id),
  KEY idx_gmud_responsavel (responsavel_id),
  CONSTRAINT fk_gmud_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
  CONSTRAINT fk_gmud_ticket FOREIGN KEY (ticket_id) REFERENCES chamados(id),
  CONSTRAINT fk_gmud_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE anexos_chamados
  ADD COLUMN gmud_id BIGINT NULL,
  ADD KEY idx_anexo_gmud (gmud_id),
  ADD CONSTRAINT fk_anexo_gmud FOREIGN KEY (gmud_id) REFERENCES gmuds(id) ON DELETE SET NULL;
