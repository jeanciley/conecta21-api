-- Execute uma vez em bancos existentes antes de iniciar a API com ddl-auto=validate.
ALTER TABLE interacoes_chamado
    ADD COLUMN tipo VARCHAR(40) NOT NULL DEFAULT 'Comentário' AFTER autor_id;
