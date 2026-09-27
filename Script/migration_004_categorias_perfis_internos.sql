USE conecta21;

CREATE TABLE perfis_customizados (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  empresa_id BIGINT NOT NULL,
  nome VARCHAR(60) NOT NULL,
  descricao VARCHAR(255) NULL,
  permissoes TEXT NOT NULL,
  ativo BIT NOT NULL DEFAULT b'1',
  UNIQUE KEY uk_perfil_empresa_nome (empresa_id, nome),
  CONSTRAINT fk_perfil_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE usuarios
  ADD COLUMN perfil_customizado_id BIGINT NULL,
  ADD COLUMN excluido_em DATETIME NULL,
  ADD CONSTRAINT fk_usuario_perfil_customizado FOREIGN KEY (perfil_customizado_id) REFERENCES perfis_customizados(id);

ALTER TABLE chamados ADD COLUMN interno BIT NOT NULL DEFAULT b'0';
