USE conecta21;

CREATE TABLE empresa_modulos (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  empresa_id BIGINT NOT NULL,
  codigo VARCHAR(60) NOT NULL,
  ativo BIT NOT NULL DEFAULT b'0',
  contratado_em DATETIME NULL,
  UNIQUE KEY uk_empresa_modulo_codigo (empresa_id, codigo),
  CONSTRAINT fk_empresa_modulos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO empresa_modulos (empresa_id, codigo, ativo, contratado_em)
SELECT id, 'GMUD', gmud_ativo, CASE WHEN gmud_ativo = b'1' THEN CURRENT_TIMESTAMP ELSE NULL END
FROM empresas
WHERE gmud_ativo = b'1';

ALTER TABLE empresas DROP COLUMN gmud_ativo;
