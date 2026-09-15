create database `conecta21` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */
/*!80016 DEFAULT ENCRYPTION='N' */;

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
                            primary key (`id`),
                            unique key `email` (`email`),
                            key `fk_usuario_empresa` (`empresa_id`),
                            constraint `fk_usuario_empresa` foreign key (`empresa_id`) references `empresas` (`id`) on
                                delete
                                cascade
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
                            `prioridade` varchar(30) not null default 'BAIXA',
                            `data_limite_resolucao` datetime default null,
                            primary key (`id`),
                            key `fk_chamado_empresa` (`empresa_id`),
                            key `fk_chamado_solicitante` (`solicitante_id`),
                            key `fk_chamado_tecnico` (`tecnico_id`),
                            constraint `fk_chamado_empresa` foreign key (`empresa_id`) references `empresas` (`id`) on
                                delete
                                cascade,
                            constraint `fk_chamado_solicitante` foreign key (`solicitante_id`) references `usuarios` (`id`),
                            constraint `fk_chamado_tecnico` foreign key (`tecnico_id`) references `usuarios` (`id`)
) engine = InnoDB auto_increment = 6 default CHARSET = utf8mb4 collate = utf8mb4_0900_ai_ci;

create table `interacoes_chamado` (
                            `id` bigint not null auto_increment,
                            `chamado_id` bigint not null,
                            `autor_id` bigint not null,
                            `mensagem` text not null,
                            `data_criacao` datetime not null default CURRENT_TIMESTAMP,
                            primary key (`id`),
                            key `idx_interacao_chamado_data` (`chamado_id`, `data_criacao`),
                            constraint `fk_interacao_chamado` foreign key (`chamado_id`) references `chamados` (`id`) on
                                delete
                                cascade,
                            constraint `fk_interacao_autor` foreign key (`autor_id`) references `usuarios` (`id`)
) engine = InnoDB default CHARSET = utf8mb4 collate = utf8mb4_0900_ai_ci;
                                cascade
) engine = InnoDB auto_increment = 3 default CHARSET = utf8mb4 collate = utf8mb4_0900_ai_ci;

CREATE TABLE categorias (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            empresa_id BIGINT NOT NULL,
                            nome VARCHAR(50) NOT NULL,
                            FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

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
                            CONSTRAINT fk_avaliacao_chamado FOREIGN KEY (chamado_id)
                                REFERENCES chamados(id) ON DELETE CASCADE
);
