USE conecta21;

CREATE TABLE prioridades (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  empresa_id BIGINT NOT NULL,
  nome VARCHAR(50) NOT NULL,
  sla_resposta_minutos INT NOT NULL,
  sla_resolucao_minutos INT NOT NULL,
  ativa BIT NOT NULL DEFAULT b'1',
  UNIQUE KEY uk_prioridade_empresa_nome (empresa_id, nome),
  CONSTRAINT fk_prioridade_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO prioridades (empresa_id, nome, sla_resposta_minutos, sla_resolucao_minutos, ativa)
SELECT e.id, 'Padrão', 240, 2880, b'1' FROM empresas e;

ALTER TABLE categorias
  ADD COLUMN prioridade_id BIGINT NULL,
  ADD COLUMN ativa BIT NOT NULL DEFAULT b'1',
  ADD CONSTRAINT fk_categoria_prioridade FOREIGN KEY (prioridade_id) REFERENCES prioridades(id);

UPDATE categorias c JOIN prioridades p ON p.empresa_id = c.empresa_id AND p.nome = 'Padrão'
SET c.prioridade_id = p.id;

ALTER TABLE chamados
  MODIFY COLUMN prioridade VARCHAR(50) NOT NULL,
  ADD COLUMN prioridade_id BIGINT NULL,
  ADD COLUMN categoria_id BIGINT NULL,
  ADD COLUMN sla_resposta_minutos_snapshot INT NULL,
  ADD COLUMN sla_resolucao_minutos_snapshot INT NULL,
  ADD COLUMN data_limite_resposta DATETIME NULL,
  ADD COLUMN data_primeira_resposta DATETIME NULL,
  ADD CONSTRAINT fk_chamado_prioridade FOREIGN KEY (prioridade_id) REFERENCES prioridades(id),
  ADD CONSTRAINT fk_chamado_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id);

UPDATE chamados ch
LEFT JOIN (SELECT chamado_id, MIN(categoria_id) categoria_id FROM chamado_categoria GROUP BY chamado_id) cc ON cc.chamado_id = ch.id
JOIN prioridades p ON p.empresa_id = ch.empresa_id AND p.nome = 'Padrão'
SET ch.categoria_id = cc.categoria_id,
    ch.prioridade_id = p.id,
    ch.sla_resposta_minutos_snapshot = 240,
    ch.sla_resolucao_minutos_snapshot = CASE ch.prioridade WHEN 'ALTA' THEN 240 WHEN 'MEDIA' THEN 1440 ELSE 2880 END,
    ch.data_limite_resposta = DATE_ADD(ch.data_abertura, INTERVAL 240 MINUTE),
    ch.data_limite_resolucao = COALESCE(ch.data_limite_resolucao,
      DATE_ADD(ch.data_abertura, INTERVAL (CASE ch.prioridade WHEN 'ALTA' THEN 240 WHEN 'MEDIA' THEN 1440 ELSE 2880 END) MINUTE));

ALTER TABLE usuarios ADD COLUMN ativo BIT NOT NULL DEFAULT b'1';

CREATE TABLE tokens_usuario (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  finalidade VARCHAR(20) NOT NULL,
  expira_em DATETIME NOT NULL,
  usado_em DATETIME NULL,
  UNIQUE KEY uk_token_hash (token_hash),
  KEY idx_token_hash (token_hash),
  CONSTRAINT fk_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
