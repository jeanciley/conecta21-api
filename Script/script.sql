CREATE DATABASE IF NOT EXISTS `conecta21`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `conecta21`;

CREATE TABLE `empresas` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `nome_fantasia` VARCHAR(100) NOT NULL,
    `cnpj` VARCHAR(18) DEFAULT NULL,
    `data_cadastro` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_empresas_cnpj` (`cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `usuarios` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `empresa_id` BIGINT NOT NULL,
    `nome` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `senha` VARCHAR(255) NOT NULL,
    `perfil` VARCHAR(20) NOT NULL,
    `data_criacao` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_usuarios_email` (`email`),
    KEY `idx_usuarios_empresa` (`empresa_id`),
    CONSTRAINT `fk_usuario_empresa`
        FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chamados` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `empresa_id` BIGINT NOT NULL,
    `solicitante_id` BIGINT NOT NULL,
    `tecnico_id` BIGINT DEFAULT NULL,
    `titulo` VARCHAR(150) NOT NULL,
    `descricao` TEXT NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ABERTO',
    `data_abertura` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `data_fechamento` DATETIME DEFAULT NULL,
    `prioridade` VARCHAR(30) NOT NULL DEFAULT 'BAIXA',
    `data_limite_resolucao` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_chamados_empresa` (`empresa_id`),
    KEY `idx_chamados_solicitante` (`solicitante_id`),
    KEY `idx_chamados_tecnico` (`tecnico_id`),
    CONSTRAINT `fk_chamado_empresa`
        FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chamado_solicitante`
        FOREIGN KEY (`solicitante_id`) REFERENCES `usuarios` (`id`),
    CONSTRAINT `fk_chamado_tecnico`
        FOREIGN KEY (`tecnico_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `interacoes_chamado` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `chamado_id` BIGINT NOT NULL,
    `autor_id` BIGINT NOT NULL,
    `mensagem` TEXT NOT NULL,
    `data_criacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_interacao_chamado_data` (`chamado_id`, `data_criacao`),
    KEY `idx_interacao_autor` (`autor_id`),
    CONSTRAINT `fk_interacao_chamado`
        FOREIGN KEY (`chamado_id`) REFERENCES `chamados` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_interacao_autor`
        FOREIGN KEY (`autor_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `categorias` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `empresa_id` BIGINT NOT NULL,
    `nome` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_categorias_empresa` (`empresa_id`),
    CONSTRAINT `fk_categoria_empresa`
        FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chamado_categoria` (
    `chamado_id` BIGINT NOT NULL,
    `categoria_id` BIGINT NOT NULL,
    PRIMARY KEY (`chamado_id`, `categoria_id`),
    KEY `idx_chamado_categoria_categoria` (`categoria_id`),
    CONSTRAINT `fk_chamado_categoria_chamado`
        FOREIGN KEY (`chamado_id`) REFERENCES `chamados` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chamado_categoria_categoria`
        FOREIGN KEY (`categoria_id`) REFERENCES `categorias` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `avaliacoes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `chamado_id` BIGINT NOT NULL,
    `nota` INT NOT NULL,
    `comentario` TEXT NULL,
    `data_criacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_avaliacao_chamado` (`chamado_id`),
    CONSTRAINT `fk_avaliacao_chamado`
        FOREIGN KEY (`chamado_id`) REFERENCES `chamados` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
