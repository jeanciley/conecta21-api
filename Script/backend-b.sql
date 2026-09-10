-- Backend B - Comunicação e Arquivos
-- Execute este script em uma base Conecta21 já existente antes de iniciar a aplicação,
-- pois spring.jpa.hibernate.ddl-auto está configurado como "validate".

CREATE TABLE IF NOT EXISTS anexos_chamados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chamado_id BIGINT NOT NULL,
    nome_original VARCHAR(255) NOT NULL,
    nome_armazenado VARCHAR(255) NOT NULL UNIQUE,
    tipo_mime VARCHAR(120),
    tamanho_bytes BIGINT NOT NULL,
    caminho_relativo VARCHAR(500) NOT NULL,
    data_upload DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_anexo_chamado
        FOREIGN KEY (chamado_id) REFERENCES chamados(id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- Normalização opcional para bases que tenham sido alimentadas pela versão antiga
-- do ChamadoService. O enum atual do projeto usa EM_ANDAMENTO e RESOLVIDO.
UPDATE chamados
SET status = 'EM_ANDAMENTO'
WHERE status = 'EM_ATENDIMENTO';

UPDATE chamados
SET status = 'RESOLVIDO'
WHERE status = 'FECHADO';
