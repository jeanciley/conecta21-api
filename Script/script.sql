-- 1. Tabela central de inquilinos (Clientes do sistema)
CREATE TABLE empresas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_fantasia VARCHAR(100) NOT NULL,
    cnpj VARCHAR(18) UNIQUE,
    data_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Tabela de identidades e acessos
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL, -- Tamanho 255 para comportar o hash do BCrypt
    perfil VARCHAR(20) NOT NULL, -- ADMIN, TECNICO, USUARIO
    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Tabela de fila e gestão de tickets
CREATE TABLE chamados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    solicitante_id BIGINT NOT NULL,
    tecnico_id BIGINT, -- Pode ser nulo até que um técnico assuma
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTO', -- ABERTO, EM_ATENDIMENTO, RESOLVIDO
    data_abertura DATETIME DEFAULT CURRENT_TIMESTAMP,
    data_fechamento DATETIME,
    CONSTRAINT fk_chamado_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT fk_chamado_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios(id),
    CONSTRAINT fk_chamado_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;