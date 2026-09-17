-- ETAPA 3 - Backend B: Gestão de Perfil
-- Executar uma única vez após a etapa 2.
-- Os arquivos do avatar ficam no mesmo armazenamento raiz usado pelos anexos.

ALTER TABLE usuarios
    ADD COLUMN avatar_nome_original VARCHAR(255) NULL AFTER data_criacao,
    ADD COLUMN avatar_tipo_mime VARCHAR(120) NULL AFTER avatar_nome_original,
    ADD COLUMN avatar_caminho_relativo VARCHAR(500) NULL AFTER avatar_tipo_mime;
