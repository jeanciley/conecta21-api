create database `conecta21` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */
/*!80016 DEFAULT ENCRYPTION='N' */;

USE `conecta21`;

create table `empresas` (
                            `id` bigint not null auto_increment,
                            `nome_fantasia` varchar(100) not null,
                            `cnpj` varchar(18) default null,
                            `data_cadastro` datetime default CURRENT_TIMESTAMP,
                            primary key (`id`),
                            unique key `cnpj` (`cnpj`)
) engine = InnoDB auto_increment = 7 default CHARSET = utf8mb4 collate = utf8mb4_0900_ai_ci;

create table `usuarios` (
                            `id` bigint not null auto_increment,
                            `empresa_id` bigint not null,
                            `nome` varchar(100) not null,
                            `email` varchar(100) not null,
                            `senha` varchar(255) not null,
                            `perfil` varchar(20) not null,
                            `data_criacao` datetime default CURRENT_TIMESTAMP,
                            `ativo` bit not null default b'1',
                            primary key (`id`),
                            unique key `email` (`email`),
                            key `fk_usuario_empresa` (`empresa_id`),
                            constraint `fk_usuario_empresa` foreign key (`empresa_id`) references `empresas` (`id`) on delete cascade
) engine = InnoDB auto_increment = 3 default CHARSET = utf8mb4 collate = utf8mb4_0900_ai_ci;

create table `chamados` (
                            `id` bigint not null auto_increment,
                            `empresa_id` bigint not null,
                            `solicitante_id` bigint not null,
                            `tecnico_id` bigint default null,
                            `titulo` varchar(150) not null,
                            `descricao` text not null,
                            `status` varchar(30) not null default 'ABERTO',
                            `data_abertura` datetime default CURRENT_TIMESTAMP,
                            `data_fechamento` datetime default null,
                            `prioridade` varchar(50) not null default 'BAIXA',
                            `data_limite_resolucao` datetime default null,
                            `prioridade_id` bigint default null,
                            `categoria_id` bigint default null,
                            `sla_resposta_minutos_snapshot` int default null,
                            `sla_resolucao_minutos_snapshot` int default null,
                            `data_limite_resposta` datetime default null,
                            `data_primeira_resposta` datetime default null,
                            primary key (`id`),
                            key `fk_chamado_empresa` (`empresa_id`),
                            key `fk_chamado_solicitante` (`solicitante_id`),
                            key `fk_chamado_tecnico` (`tecnico_id`),
                            constraint `fk_chamado_empresa` foreign key (`empresa_id`) references `empresas` (`id`) on delete cascade,
                            constraint `fk_chamado_solicitante` foreign key (`solicitante_id`) references `usuarios` (`id`),
                            constraint `fk_chamado_tecnico` foreign key (`tecnico_id`) references `usuarios` (`id`)
) engine = InnoDB auto_increment = 6 default CHARSET = utf8mb4 collate = utf8mb4_0900_ai_ci;

CREATE TABLE `prioridades` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `empresa_id` BIGINT NOT NULL,
  `nome` VARCHAR(50) NOT NULL,
  `sla_resposta_minutos` INT NOT NULL,
  `sla_resolucao_minutos` INT NOT NULL,
  `ativa` BIT NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prioridade_empresa_nome` (`empresa_id`, `nome`),
  CONSTRAINT `fk_prioridade_empresa` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

create table `interacoes_chamado` (
                                      `id` bigint not null auto_increment,
                                      `chamado_id` bigint not null,
                                      `autor_id` bigint not null,
                                      `tipo` varchar(40) not null default 'Comentário',
                                      `mensagem` text not null,
                                      `data_criacao` datetime not null default CURRENT_TIMESTAMP,
                                      primary key (`id`),
                                      key `idx_interacao_chamado_data` (`chamado_id`, `data_criacao`),
                                      constraint `fk_interacao_chamado` foreign key (`chamado_id`) references `chamados` (`id`) on delete cascade,
                                      constraint `fk_interacao_autor` foreign key (`autor_id`) references `usuarios` (`id`)
) engine = InnoDB default CHARSET = utf8mb4 collate = utf8mb4_0900_ai_ci;

CREATE TABLE anexos_interacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interacao_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    tamanho BIGINT NOT NULL,
    conteudo MEDIUMBLOB NOT NULL,
    CONSTRAINT fk_anexo_interacao FOREIGN KEY (interacao_id)
        REFERENCES interacoes_chamado(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categorias (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            empresa_id BIGINT NOT NULL,
                            nome VARCHAR(50) NOT NULL,
                            prioridade_id BIGINT NULL,
                            ativa BIT NOT NULL DEFAULT b'1',
                            CONSTRAINT fk_categoria_prioridade FOREIGN KEY (prioridade_id) REFERENCES prioridades(id),
                            FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

CREATE TABLE `tokens_usuario` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `usuario_id` BIGINT NOT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `finalidade` VARCHAR(20) NOT NULL,
  `expira_em` DATETIME NOT NULL,
  `usado_em` DATETIME NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_token_hash` (`token_hash`), KEY `idx_token_hash` (`token_hash`),
  CONSTRAINT `fk_token_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE chamados
  ADD CONSTRAINT fk_chamado_prioridade FOREIGN KEY (prioridade_id) REFERENCES prioridades(id),
  ADD CONSTRAINT fk_chamado_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id);

CREATE TABLE chamado_categoria (
                                   chamado_id BIGINT NOT NULL,
                                   categoria_id BIGINT NOT NULL,
                                   PRIMARY KEY (chamado_id, categoria_id),
                                   FOREIGN KEY (chamado_id) REFERENCES chamados(id),
                                   FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE TABLE avaliacoes (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            chamado_id BIGINT NOT NULL UNIQUE,
                            nota INT NOT NULL,
                            comentario TEXT NULL,
                            data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_avaliacao_chamado FOREIGN KEY (chamado_id) REFERENCES chamados(id) ON DELETE CASCADE
);

CREATE TABLE artigos_faq (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             empresa_id BIGINT NOT NULL,
                             autor_id BIGINT NOT NULL,
                             categoria_id BIGINT NOT NULL,
                             titulo VARCHAR(150) NOT NULL,
                             conteudo TEXT NOT NULL,
                             data_criacao DATETIME,
                             FOREIGN KEY (empresa_id) REFERENCES empresas(id),
                             FOREIGN KEY (autor_id) REFERENCES usuarios(id),
                             CONSTRAINT fk_artigo_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);
