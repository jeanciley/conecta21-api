-- Dados demonstrativos da Vértice Sistemas (empresa 7).
-- Compatível com o schema atual do Conecta21 (MySQL 8+).
-- Reexecutável: usuários, perfis, prioridades, categorias, módulos e conteúdo
-- são localizados por chaves naturais antes de inserir.
-- Usuários criados usam a senha demonstrativa "password".

USE conecta21;
SET NAMES utf8mb4;
SET @empresa_id := 7;
SET @usuario_base_id := 3;

START TRANSACTION;

-- Confirma os registros informados pelo solicitante e nomeia a empresa de teste.
UPDATE empresas
SET nome_fantasia = 'Vértice Sistemas'
WHERE id = @empresa_id;

-- O usuário principal já existente é o administrador da demonstração.
UPDATE usuarios
SET perfil = 'ADMIN', perfil_customizado_id = NULL, ativo = b'1',
    excluido = b'0', excluido_em = NULL
WHERE id = @usuario_base_id AND empresa_id = @empresa_id;

-- A senha é um hash BCrypt válido para "password".
-- O usuário de id 3 é preservado; estes são membros fictícios adicionais.
INSERT INTO usuarios (empresa_id, nome, email, senha, perfil, ativo, excluido)
SELECT @empresa_id, dados.nome, dados.email,
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       dados.perfil, b'1', b'0'
FROM (
    SELECT 'Marina Costa' AS nome, 'marina.costa@vertice.demo' AS email, 'TECNICO' AS perfil
    UNION ALL SELECT 'Rafael Nunes', 'rafael.nunes@vertice.demo', 'TECNICO'
    UNION ALL SELECT 'Camila Rocha', 'camila.rocha@vertice.demo', 'TECNICO'
    UNION ALL SELECT 'Bruno Almeida', 'bruno.almeida@vertice.demo', 'USUARIO'
    UNION ALL SELECT 'Larissa Martins', 'larissa.martins@vertice.demo', 'USUARIO'
    UNION ALL SELECT 'Diego Ferreira', 'diego.ferreira@vertice.demo', 'USUARIO'
    UNION ALL SELECT 'Paula Ribeiro', 'paula.ribeiro@vertice.demo', 'USUARIO'
    UNION ALL SELECT 'Eduardo Lima', 'eduardo.lima@vertice.demo', 'USUARIO'
) AS dados
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios u
    WHERE u.empresa_id = @empresa_id AND u.email = dados.email
);

-- Perfis customizados para simular solicitantes e gestores internos.
INSERT INTO perfis_customizados (empresa_id, nome, descricao, permissoes, ativo)
SELECT @empresa_id, dados.nome, dados.descricao, dados.permissoes, b'1'
FROM (
    SELECT 'Colaborador' AS nome,
           'Solicita atendimento interno e acompanha os próprios chamados.' AS descricao,
           'CHAMADOS_INTERNOS' AS permissoes
    UNION ALL
    SELECT 'Gestor de área',
           'Acompanha chamados internos e pode consultar relatórios.',
           'CHAMADOS_INTERNOS,GERAR_RELATORIOS'
) AS dados
WHERE NOT EXISTS (
    SELECT 1 FROM perfis_customizados p
    WHERE p.empresa_id = @empresa_id AND p.nome = dados.nome
);

UPDATE usuarios u
JOIN perfis_customizados p
  ON p.empresa_id = u.empresa_id
 AND p.nome = CASE
     WHEN u.email IN ('bruno.almeida@vertice.demo', 'larissa.martins@vertice.demo',
                      'diego.ferreira@vertice.demo', 'eduardo.lima@vertice.demo')
       THEN 'Colaborador'
     WHEN u.email = 'paula.ribeiro@vertice.demo' THEN 'Gestor de área'
     ELSE NULL
 END
SET u.perfil_customizado_id = p.id,
    u.perfil = 'USUARIO'
WHERE u.empresa_id = @empresa_id
  AND u.email IN ('bruno.almeida@vertice.demo', 'larissa.martins@vertice.demo',
                  'diego.ferreira@vertice.demo', 'eduardo.lima@vertice.demo',
                  'paula.ribeiro@vertice.demo');

-- SLA (resposta / resolução em minutos).
INSERT INTO prioridades (empresa_id, nome, sla_resposta_minutos, sla_resolucao_minutos, ativa)
SELECT @empresa_id, dados.nome, dados.resposta, dados.resolucao, b'1'
FROM (
    SELECT 'Baixa' AS nome, 480 AS resposta, 2880 AS resolucao
    UNION ALL SELECT 'Média', 240, 1440
    UNION ALL SELECT 'Alta', 60, 480
    UNION ALL SELECT 'Crítica', 15, 120
) AS dados
WHERE NOT EXISTS (
    SELECT 1 FROM prioridades p
    WHERE p.empresa_id = @empresa_id AND p.nome = dados.nome
);

UPDATE prioridades p
JOIN (
    SELECT 'Baixa' AS nome, 480 AS resposta, 2880 AS resolucao
    UNION ALL SELECT 'Média', 240, 1440
    UNION ALL SELECT 'Alta', 60, 480
    UNION ALL SELECT 'Crítica', 15, 120
) dados ON dados.nome = p.nome
SET p.sla_resposta_minutos = dados.resposta,
    p.sla_resolucao_minutos = dados.resolucao,
    p.ativa = b'1'
WHERE p.empresa_id = @empresa_id;

-- Categorias representam filas/departamentos do CSC e tipos de atendimento.
INSERT INTO categorias (empresa_id, nome, prioridade_id, ativa)
SELECT @empresa_id, dados.nome, p.id, b'1'
FROM (
    SELECT 'TI Interna - Infraestrutura e Acessos' AS nome, 'Alta' AS prioridade
    UNION ALL SELECT 'TI Interna - Equipamentos', 'Média'
    UNION ALL SELECT 'Gente & Gestão - Benefícios', 'Média'
    UNION ALL SELECT 'Gente & Gestão - Onboarding', 'Alta'
    UNION ALL SELECT 'Facilities - Escritório', 'Média'
    UNION ALL SELECT 'Financeiro Interno', 'Baixa'
    UNION ALL SELECT 'ERP Cloud - Suporte', 'Alta'
    UNION ALL SELECT 'ERP Cloud - Integrações', 'Crítica'
) AS dados
JOIN prioridades p ON p.empresa_id = @empresa_id AND p.nome = dados.prioridade
WHERE NOT EXISTS (
    SELECT 1 FROM categorias c
    WHERE c.empresa_id = @empresa_id AND c.nome = dados.nome
);

UPDATE categorias c
JOIN (
    SELECT 'TI Interna - Infraestrutura e Acessos' AS nome, 'Alta' AS prioridade
    UNION ALL SELECT 'TI Interna - Equipamentos', 'Média'
    UNION ALL SELECT 'Gente & Gestão - Benefícios', 'Média'
    UNION ALL SELECT 'Gente & Gestão - Onboarding', 'Alta'
    UNION ALL SELECT 'Facilities - Escritório', 'Média'
    UNION ALL SELECT 'Financeiro Interno', 'Baixa'
    UNION ALL SELECT 'ERP Cloud - Suporte', 'Alta'
    UNION ALL SELECT 'ERP Cloud - Integrações', 'Crítica'
) dados ON dados.nome = c.nome
JOIN prioridades p ON p.empresa_id = @empresa_id AND p.nome = dados.prioridade
SET c.prioridade_id = p.id, c.ativa = b'1'
WHERE c.empresa_id = @empresa_id;

-- Contrata o módulo GMUD para que os controles correspondentes apareçam.
INSERT INTO empresa_modulos (empresa_id, codigo, ativo, contratado_em)
SELECT @empresa_id, 'GMUD', b'1', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM empresa_modulos em
    WHERE em.empresa_id = @empresa_id AND em.codigo = 'GMUD'
);
UPDATE empresa_modulos
SET ativo = b'1', contratado_em = COALESCE(contratado_em, NOW())
WHERE empresa_id = @empresa_id AND codigo = 'GMUD';

-- Chamados recentes para dashboard, filas, filtros, relatórios e detalhes.
-- As descrições usam somente informações compatíveis com o modelo atual.
INSERT INTO chamados (
    empresa_id, solicitante_id, tecnico_id, titulo, descricao, status,
    data_abertura, data_fechamento, prioridade, interno, tipo, excluido,
    data_limite_resolucao, prioridade_id, categoria_id,
    sla_resposta_minutos_snapshot, sla_resolucao_minutos_snapshot,
    data_limite_resposta, data_primeira_resposta
)
SELECT @empresa_id, solicitante.id, tecnico.id, dados.titulo, dados.descricao,
       dados.status, DATE_SUB(NOW(), INTERVAL dados.dias DAY),
       CASE WHEN dados.status = 'RESOLVIDO'
            THEN DATE_ADD(DATE_SUB(NOW(), INTERVAL dados.dias DAY), INTERVAL dados.horas_resolucao HOUR)
            ELSE NULL END,
       dados.prioridade, dados.interno, dados.tipo, b'0',
       DATE_ADD(DATE_SUB(NOW(), INTERVAL dados.dias DAY), INTERVAL p.sla_resolucao_minutos MINUTE),
       p.id, c.id, p.sla_resposta_minutos, p.sla_resolucao_minutos,
       DATE_ADD(DATE_SUB(NOW(), INTERVAL dados.dias DAY), INTERVAL p.sla_resposta_minutos MINUTE),
       DATE_ADD(DATE_SUB(NOW(), INTERVAL dados.dias DAY), INTERVAL dados.horas_resposta HOUR)
FROM (
    SELECT 'Onboarding: acessos e notebook para Desenvolvedor Pleno' AS titulo,
           'Preparar notebook e liberar GitHub, AWS, VPN e e-mail para início na próxima segunda-feira.' AS descricao,
           'EM_ANDAMENTO' AS status, 2 AS dias, 8 AS horas_resolucao, 1 AS horas_resposta,
           'Alta' AS prioridade, 'TI Interna - Onboarding' AS categoria,
           'larissa.martins@vertice.demo' AS solicitante, 'marina.costa@vertice.demo' AS tecnico,
           b'1' AS interno, 'TI_INTERNO' AS tipo
    UNION ALL SELECT 'Ar-condicionado da Sala de Reunião 03 pingando',
           'Há gotejamento próximo à mesa de videoconferência; solicitamos vistoria antes da reunião com cliente.',
           'ABERTO', 1, 0, 0, 'Média', 'Facilities - Escritório', 'bruno.almeida@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'FACILITIES'
    UNION ALL SELECT 'Dúvida sobre coparticipação no plano de saúde',
           'Poderiam explicar os lançamentos de coparticipação que apareceram no holerite deste mês?',
           'RESOLVIDO', 8, 22, 1, 'Baixa', 'Gente & Gestão - Benefícios', 'diego.ferreira@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Acesso ao ambiente de homologação do ERP',
           'Analista de produto precisa do perfil de consulta no ambiente de homologação até o fim do dia.',
           'EM_ANDAMENTO', 3, 10, 2, 'Alta', 'TI Interna - Infraestrutura e Acessos', 'eduardo.lima@vertice.demo', 'marina.costa@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Reembolso de deslocamento para visita técnica',
           'Anexar comprovantes e confirmar o prazo de pagamento do reembolso.',
           'ABERTO', 4, 0, 0, 'Baixa', 'Financeiro Interno', 'paula.ribeiro@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Notebook reiniciando durante compilação',
           'Equipamento reinicia ao compilar o projeto; verificar memória e saúde do SSD.',
           'RESOLVIDO', 12, 6, 1, 'Média', 'TI Interna - Equipamentos', 'larissa.martins@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Permissão para repositório de integrações',
           'Solicito acesso de leitura e escrita ao repositório das integrações bancárias.',
           'ABERTO', 6, 0, 0, 'Alta', 'TI Interna - Infraestrutura e Acessos', 'bruno.almeida@vertice.demo', 'marina.costa@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Falha ao importar arquivo de retorno bancário',
           'Cliente demonstrativo: Nova Aurora Distribuição | Módulo: Financeiro | Ambiente: Produção. O arquivo CNAB é rejeitado após a atualização de ontem; anexar evidência e validar o layout bancário.',
           'EM_ATRASO', 9, 0, 12, 'Crítica', 'ERP Cloud - Integrações', 'paula.ribeiro@vertice.demo', 'rafael.nunes@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Relatório de estoque diverge do saldo por depósito',
           'Cliente demonstrativo: Mercado Horizonte | Módulo: Estoque | Ambiente: Produção. O saldo consolidado não confere com a soma dos depósitos após transferência entre filiais.',
           'EM_ANDAMENTO', 5, 0, 2, 'Alta', 'ERP Cloud - Suporte', 'diego.ferreira@vertice.demo', 'camila.rocha@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Erro 502 no portal de pedidos',
           'Cliente demonstrativo: Lume Materiais | Módulo: Vendas | Ambiente: Produção. O portal retorna erro 502 de forma intermitente desde 10h; impacto em todos os representantes.',
           'RESOLVIDO', 18, 3, 1, 'Crítica', 'ERP Cloud - Suporte', 'eduardo.lima@vertice.demo', 'marina.costa@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Solicitação de acesso VPN para nova analista',
           'Criar acesso VPN com autenticação multifator e perfil padrão de engenharia de dados.',
           'ABERTO', 0, 0, 0, 'Alta', 'TI Interna - Infraestrutura e Acessos', 'larissa.martins@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Solicitação de segunda via do cartão corporativo',
           'Cartão corporativo danificado; confirmar bloqueio e prazo para emissão de segunda via.',
           'EM_ANDAMENTO', 14, 15, 3, 'Média', 'Financeiro Interno', 'paula.ribeiro@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Solicitação de cadeira ergonômica',
           'A cadeira atual apresenta falha no ajuste de altura; solicitar avaliação e substituição.',
           'RESOLVIDO', 21, 30, 5, 'Baixa', 'Facilities - Escritório', 'eduardo.lima@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'FACILITIES'
    UNION ALL SELECT 'Cadastro de novo centro de custo no ERP',
           'Cliente demonstrativo: Ativa Logística | Módulo: Financeiro | Ambiente: Homologação. Criar centro de custo "Operações Sul" e disponibilizar para os lançamentos de teste.',
           'ABERTO', 7, 0, 0, 'Média', 'ERP Cloud - Suporte', 'bruno.almeida@vertice.demo', 'marina.costa@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Dúvida sobre desconto de vale-refeição',
           'Confirmar como funciona o desconto proporcional em mês com férias.',
           'RESOLVIDO', 25, 18, 2, 'Baixa', 'Gente & Gestão - Benefícios', 'diego.ferreira@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Integração fiscal não transmite notas de serviço',
           'Cliente demonstrativo: Nova Aurora Distribuição | Módulo: Fiscal | Ambiente: Produção. Notas de serviço ficam em fila com retorno de certificado inválido após renovação.',
           'EM_ATRASO', 11, 0, 8, 'Alta', 'ERP Cloud - Integrações', 'paula.ribeiro@vertice.demo', 'marina.costa@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Ajuste de iluminação no posto de trabalho 24',
           'A luminária está piscando e prejudica as chamadas de vídeo.',
           'ABERTO', 2, 0, 0, 'Baixa', 'Facilities - Escritório', 'bruno.almeida@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'FACILITIES'
    UNION ALL SELECT 'Perfil de acesso para fechamento contábil',
           'Habilitar perfil temporário de fechamento contábil até o quinto dia útil.',
           'EM_ANDAMENTO', 1, 7, 1, 'Alta', 'TI Interna - Infraestrutura e Acessos', 'paula.ribeiro@vertice.demo', 'marina.costa@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Consulta de pedidos demora mais de 30 segundos',
           'Cliente demonstrativo: Mercado Horizonte | Módulo: Vendas | Ambiente: Produção. A consulta por período está lenta para mais de 90 dias; avaliar plano de execução e sugerir filtro padrão.',
           'ABERTO', 3, 0, 0, 'Média', 'ERP Cloud - Suporte', 'eduardo.lima@vertice.demo', 'rafael.nunes@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Liberação de acesso ao GitHub para estagiário',
           'Criar conta, adicionar à organização e aplicar equipes de onboarding.',
           'RESOLVIDO', 29, 9, 2, 'Média', 'TI Interna - Onboarding', 'larissa.martins@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Divergência na apuração de comissão de vendas',
           'Cliente demonstrativo: Lume Materiais | Módulo: Vendas | Ambiente: Produção. Comissão não considera devoluções lançadas no mesmo período; validar regra e orientar reprocessamento.',
           'EM_ANDAMENTO', 13, 0, 4, 'Alta', 'ERP Cloud - Suporte', 'diego.ferreira@vertice.demo', 'marina.costa@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Novo usuário não recebe convite do ERP',
           'Cliente demonstrativo: Ponto Norte Atacado | Módulo: Administração | Ambiente: Produção. O convite é exibido como enviado, mas não chega à caixa de entrada nem ao spam.',
           'ABERTO', 1, 0, 0, 'Média', 'ERP Cloud - Suporte', 'bruno.almeida@vertice.demo', 'rafael.nunes@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Fechamento mensal apresenta diferença de centavos',
           'Cliente demonstrativo: Horizonte Peças | Módulo: Financeiro | Ambiente: Homologação. A soma dos títulos difere do total do relatório em R$ 0,03; encaminhamos os lançamentos usados no teste.',
           'EM_ANDAMENTO', 2, 0, 2, 'Média', 'ERP Cloud - Suporte', 'paula.ribeiro@vertice.demo', 'camila.rocha@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Atualização do certificado da integração municipal',
           'Cliente demonstrativo: Vila Verde Serviços | Módulo: Fiscal | Ambiente: Homologação. Agendar a troca do certificado e validar a emissão de uma nota de serviço após a atualização.',
           'ABERTO', 4, 0, 0, 'Alta', 'ERP Cloud - Integrações', 'eduardo.lima@vertice.demo', 'marina.costa@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Pedido duplicado após instabilidade no checkout',
           'Cliente demonstrativo: Lume Materiais | Módulo: Vendas | Ambiente: Produção. Dois pedidos foram criados para a mesma tentativa de compra; verificar idempotência e orientar cancelamento de um deles.',
           'RESOLVIDO', 10, 5, 1, 'Alta', 'ERP Cloud - Suporte', 'diego.ferreira@vertice.demo', 'rafael.nunes@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Lentidão ao gerar arquivo de remessa',
           'Cliente demonstrativo: Ativa Logística | Módulo: Financeiro | Ambiente: Produção. A geração do arquivo de remessa leva cerca de quatro minutos para lotes com mais de mil títulos.',
           'EM_ATRASO', 16, 0, 10, 'Alta', 'ERP Cloud - Integrações', 'paula.ribeiro@vertice.demo', 'marina.costa@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Campo de observação não aparece na impressão do pedido',
           'Cliente demonstrativo: Ponto Norte Atacado | Módulo: Vendas | Ambiente: Homologação. O campo é salvo corretamente, mas não aparece no PDF usado pela expedição.',
           'ABERTO', 6, 0, 0, 'Baixa', 'ERP Cloud - Suporte', 'bruno.almeida@vertice.demo', 'camila.rocha@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Webhook de atualização de estoque retorna 401',
           'Cliente demonstrativo: Mercado Horizonte | Módulo: Integrações | Ambiente: Produção. O endpoint começou a rejeitar chamadas depois da rotação da credencial; validar configuração e reenvio dos eventos pendentes.',
           'EM_ANDAMENTO', 3, 0, 1, 'Crítica', 'ERP Cloud - Integrações', 'eduardo.lima@vertice.demo', 'rafael.nunes@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Relatório de contas a pagar não filtra filial',
           'Cliente demonstrativo: Nova Aurora Distribuição | Módulo: Financeiro | Ambiente: Produção. O filtro de filial é ignorado quando combinado com o período de vencimento.',
           'RESOLVIDO', 23, 12, 3, 'Média', 'ERP Cloud - Suporte', 'paula.ribeiro@vertice.demo', 'camila.rocha@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Falha ao sincronizar catálogo de produtos',
           'Cliente demonstrativo: Horizonte Peças | Módulo: Integrações | Ambiente: Produção. A sincronização parou no produto 8.412; os demais itens permanecem sem atualização desde ontem.',
           'EM_ATRASO', 19, 0, 9, 'Crítica', 'ERP Cloud - Integrações', 'diego.ferreira@vertice.demo', 'marina.costa@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Solicitação de usuário somente para consulta',
           'Cliente demonstrativo: Vila Verde Serviços | Módulo: Administração | Ambiente: Produção. Criar um perfil somente leitura para auditoria interna, sem acesso a dados de folha.',
           'RESOLVIDO', 27, 16, 2, 'Baixa', 'ERP Cloud - Suporte', 'bruno.almeida@vertice.demo', 'rafael.nunes@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Erro ao cancelar nota fiscal em contingência',
           'Cliente demonstrativo: Ativa Logística | Módulo: Fiscal | Ambiente: Produção. A nota foi emitida em contingência e o cancelamento retorna protocolo inexistente; avaliar orientação da SEFAZ.',
           'EM_ANDAMENTO', 8, 0, 4, 'Alta', 'ERP Cloud - Integrações', 'eduardo.lima@vertice.demo', 'camila.rocha@vertice.demo', b'0', 'SUPORTE_EXTERNO'
    UNION ALL SELECT 'Solicitação de monitor adicional para estação de trabalho',
           'A equipe de Produto precisa de um monitor adicional para validar fluxos em duas resoluções simultaneamente. Informar disponibilidade e previsão de entrega.',
           'ABERTO', 5, 0, 0, 'Baixa', 'TI Interna - Equipamentos', 'eduardo.lima@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Acesso ao painel de métricas do produto',
           'Liberar acesso de leitura ao painel de métricas para a nova analista de produto. O acesso deve usar a conta corporativa e autenticação multifator.',
           'RESOLVIDO', 15, 7, 1, 'Média', 'TI Interna - Infraestrutura e Acessos', 'larissa.martins@vertice.demo', 'marina.costa@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Dúvida sobre inclusão de dependente no benefício',
           'Como incluir um dependente no plano de saúde e quais documentos precisam ser enviados? A solicitação pode ser feita fora do período de admissão?',
           'ABERTO', 3, 0, 0, 'Baixa', 'Gente & Gestão - Benefícios', 'bruno.almeida@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Ajuste de acesso para retorno de licença',
           'Restabelecer os acessos corporativos para colaboradora que retorna de licença na próxima semana. Confirmar e-mail, VPN e ferramentas do time.',
           'EM_ANDAMENTO', 7, 9, 2, 'Alta', 'Gente & Gestão - Onboarding', 'diego.ferreira@vertice.demo', 'marina.costa@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Vazamento leve próximo ao bebedouro do segundo andar',
           'Foi observado acúmulo de água no piso próximo ao bebedouro. Isolar a área e verificar a conexão hidráulica para evitar escorregões.',
           'RESOLVIDO', 9, 4, 1, 'Alta', 'Facilities - Escritório', 'paula.ribeiro@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'FACILITIES'
    UNION ALL SELECT 'Reserva da sala de treinamento não aparece no calendário',
           'A reserva da sala de treinamento foi confirmada, mas não aparece no calendário compartilhado. Verificar sincronização e evitar conflito com outra equipe.',
           'ABERTO', 11, 0, 0, 'Baixa', 'Facilities - Escritório', 'bruno.almeida@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'FACILITIES'
    UNION ALL SELECT 'Reembolso de certificação profissional',
           'Encaminho comprovante da certificação aprovada para reembolso. Confirmar se é necessário enviar também o certificado de conclusão.',
           'EM_ANDAMENTO', 6, 10, 2, 'Média', 'Financeiro Interno', 'larissa.martins@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Divergência no valor de auxílio mobilidade',
           'O valor creditado neste mês está diferente do informado no portal de benefícios. Solicito conferência do cálculo e orientação sobre o próximo ciclo.',
           'RESOLVIDO', 20, 14, 2, 'Baixa', 'Gente & Gestão - Benefícios', 'eduardo.lima@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Acesso temporário para auditoria de repositório',
           'Conceder acesso de leitura ao repositório de autenticação para revisão de segurança durante esta semana e remover a permissão ao final da análise.',
           'ABERTO', 1, 0, 0, 'Alta', 'TI Interna - Infraestrutura e Acessos', 'paula.ribeiro@vertice.demo', 'marina.costa@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Troca de teclado com teclas intermitentes',
           'Algumas teclas do teclado da estação de atendimento falham de forma intermitente. Solicito teste e substituição se necessário.',
           'RESOLVIDO', 26, 8, 1, 'Baixa', 'TI Interna - Equipamentos', 'diego.ferreira@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Dúvida sobre prazo de pagamento de despesas',
           'Qual é o prazo previsto para pagamento de despesas aprovadas e como acompanhar o status da solicitação no portal interno?',
           'ABERTO', 0, 0, 0, 'Baixa', 'Financeiro Interno', 'bruno.almeida@vertice.demo', 'rafael.nunes@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Configuração de impressora na área de atendimento',
           'A impressora compartilhada da área de atendimento não aparece na lista de dispositivos após a troca de estação. Configurar driver e fila de impressão.',
           'EM_ANDAMENTO', 12, 11, 2, 'Média', 'TI Interna - Equipamentos', 'larissa.martins@vertice.demo', 'camila.rocha@vertice.demo', b'1', 'TI_INTERNO'
    UNION ALL SELECT 'Solicitação de ajuste no acesso ao sistema de ponto',
           'O sistema de ponto informa unidade incorreta para colaborador em trabalho híbrido. Corrigir a unidade vinculada para permitir o registro correto.',
           'RESOLVIDO', 17, 5, 1, 'Média', 'Gente & Gestão - Onboarding', 'paula.ribeiro@vertice.demo', 'marina.costa@vertice.demo', b'1', 'TI_INTERNO'
) AS dados
JOIN usuarios solicitante ON solicitante.empresa_id = @empresa_id AND solicitante.email = dados.solicitante
JOIN usuarios tecnico ON tecnico.empresa_id = @empresa_id AND tecnico.email = dados.tecnico
JOIN categorias c ON c.empresa_id = @empresa_id AND c.nome = dados.categoria
JOIN prioridades p ON p.id = c.prioridade_id
WHERE NOT EXISTS (
    SELECT 1 FROM chamados ch
    WHERE ch.empresa_id = @empresa_id AND ch.titulo = dados.titulo
);

-- Conversas para popular a linha do tempo e detalhes dos chamados.
INSERT INTO interacoes_chamado (chamado_id, autor_id, tipo, mensagem, data_criacao)
SELECT ch.id, u.id, 'Comentário', dados.mensagem,
       DATE_ADD(ch.data_abertura, INTERVAL dados.horas HOUR)
FROM (
    SELECT 'Onboarding: acessos e notebook para Desenvolvedor Pleno' AS titulo,
           'marina.costa@vertice.demo' AS email, 'A solicitação foi recebida. Estou preparando os acessos e confirmando a imagem padrão do notebook com Engenharia.' AS mensagem, 1 AS horas
    UNION ALL SELECT 'Onboarding: acessos e notebook para Desenvolvedor Pleno', 'larissa.martins@vertice.demo', 'A conta de e-mail e o GitHub já estão liberados. AWS e VPN seguem em validação com Infra.', 3
    UNION ALL SELECT 'Ar-condicionado da Sala de Reunião 03 pingando', 'camila.rocha@vertice.demo', 'Facilities foi acionado e fará a vistoria hoje às 14h. A sala foi sinalizada para evitar uso até a inspeção.', 2
    UNION ALL SELECT 'Dúvida sobre coparticipação no plano de saúde', 'rafael.nunes@vertice.demo', 'A coparticipação corresponde aos procedimentos utilizados no mês anterior. Encaminhei o demonstrativo para o e-mail corporativo.', 4
    UNION ALL SELECT 'Falha ao importar arquivo de retorno bancário', 'rafael.nunes@vertice.demo', 'Estamos reproduzindo o retorno em homologação. O impacto está restrito à conciliação automática; os lançamentos manuais seguem disponíveis.', 3
    UNION ALL SELECT 'Erro 502 no portal de pedidos', 'marina.costa@vertice.demo', 'Ajustamos o pool de conexões e monitoramos o portal. O acesso foi normalizado às 11h20.', 2
    UNION ALL SELECT 'Solicitação de cadeira ergonômica', 'camila.rocha@vertice.demo', 'A cadeira foi substituída e o patrimônio atualizado no inventário de Facilities.', 6
    UNION ALL SELECT 'Integração fiscal não transmite notas de serviço', 'marina.costa@vertice.demo', 'Certificado renovado no ambiente de homologação. Aguardamos validação do cliente antes da atualização em produção.', 5
    UNION ALL SELECT 'Integração fiscal não transmite notas de serviço', 'paula.ribeiro@vertice.demo', 'Validação aprovada. Podemos agendar a atualização fora do horário comercial.', 7
    UNION ALL SELECT 'Divergência na apuração de comissão de vendas', 'marina.costa@vertice.demo', 'Identificamos a regra de devolução aplicada no período seguinte. Enviamos cenário de teste para validação.', 4
    UNION ALL SELECT 'Novo usuário não recebe convite do ERP', 'rafael.nunes@vertice.demo', 'Estamos verificando o serviço de envio e os registros de entrega do domínio informado.', 2
    UNION ALL SELECT 'Fechamento mensal apresenta diferença de centavos', 'camila.rocha@vertice.demo', 'Reproduzimos a diferença com arredondamento por item. Vamos comparar o relatório detalhado com o consolidado.', 3
    UNION ALL SELECT 'Atualização do certificado da integração municipal', 'marina.costa@vertice.demo', 'A atualização foi aplicada em homologação. A emissão de teste foi concluída e aguardamos a janela aprovada para produção.', 5
    UNION ALL SELECT 'Pedido duplicado após instabilidade no checkout', 'rafael.nunes@vertice.demo', 'Ajustamos a proteção contra reenvio e identificamos os dois pedidos para cancelamento seguro.', 2
    UNION ALL SELECT 'Webhook de atualização de estoque retorna 401', 'rafael.nunes@vertice.demo', 'A nova credencial foi configurada e os eventos pendentes começaram a ser reenviados.', 1
    UNION ALL SELECT 'Relatório de contas a pagar não filtra filial', 'camila.rocha@vertice.demo', 'O filtro foi corrigido e validado com duas filiais. A versão atualizada já está disponível.', 4
    UNION ALL SELECT 'Solicitação de usuário somente para consulta', 'rafael.nunes@vertice.demo', 'O perfil de consulta foi criado e testado sem permissões de edição ou acesso a folha.', 3
    UNION ALL SELECT 'Solicitação de monitor adicional para estação de trabalho', 'camila.rocha@vertice.demo', 'Solicitação registrada com Facilities. Vamos atualizar o chamado quando houver previsão de entrega.', 2
    UNION ALL SELECT 'Acesso ao painel de métricas do produto', 'marina.costa@vertice.demo', 'A permissão de leitura foi liberada e confirmada com a solicitante.', 3
    UNION ALL SELECT 'Dúvida sobre inclusão de dependente no benefício', 'rafael.nunes@vertice.demo', 'A inclusão pode ser solicitada pelo portal de benefícios. Enviaremos a lista de documentos pelo canal corporativo.', 4
    UNION ALL SELECT 'Vazamento leve próximo ao bebedouro do segundo andar', 'camila.rocha@vertice.demo', 'A conexão foi ajustada e o piso seco. O bebedouro voltou a funcionar normalmente.', 3
    UNION ALL SELECT 'Reembolso de certificação profissional', 'rafael.nunes@vertice.demo', 'O comprovante foi recebido. A equipe financeira está conferindo a aprovação e o ciclo de pagamento.', 4
    UNION ALL SELECT 'Acesso temporário para auditoria de repositório', 'marina.costa@vertice.demo', 'O acesso temporário foi concedido com escopo somente leitura. A remoção ficará agendada para o fim da semana.', 2
    UNION ALL SELECT 'Configuração de impressora na área de atendimento', 'camila.rocha@vertice.demo', 'O driver foi reinstalado e a página de teste foi impressa com sucesso.', 3
) dados
JOIN chamados ch ON ch.empresa_id = @empresa_id AND ch.titulo = dados.titulo
JOIN usuarios u ON u.empresa_id = @empresa_id AND u.email = dados.email
WHERE NOT EXISTS (
    SELECT 1 FROM interacoes_chamado i
    WHERE i.chamado_id = ch.id AND i.autor_id = u.id AND i.mensagem = dados.mensagem
);

-- Avaliações apenas em chamados resolvidos.
INSERT INTO avaliacoes (chamado_id, nota, comentario, data_criacao)
SELECT ch.id, dados.nota, dados.comentario, DATE_ADD(ch.data_fechamento, INTERVAL 1 HOUR)
FROM (
    SELECT 'Dúvida sobre coparticipação no plano de saúde' AS titulo, 5 AS nota, 'Explicação clara e retorno rápido.' AS comentario
    UNION ALL SELECT 'Notebook reiniciando durante compilação', 4, 'Equipamento estabilizado; atendimento atencioso.'
    UNION ALL SELECT 'Erro 502 no portal de pedidos', 5, 'Portal voltou rapidamente e recebemos atualização durante a análise.'
    UNION ALL SELECT 'Solicitação de cadeira ergonômica', 5, 'Troca realizada conforme combinado.'
    UNION ALL SELECT 'Dúvida sobre desconto de vale-refeição', 4, 'Orientação objetiva sobre o cálculo.'
    UNION ALL SELECT 'Liberação de acesso ao GitHub para estagiário', 5, 'Acessos estavam prontos no primeiro dia.'
    UNION ALL SELECT 'Pedido duplicado após instabilidade no checkout', 4, 'Recebemos retorno sobre a correção e os pedidos foram tratados.'
    UNION ALL SELECT 'Relatório de contas a pagar não filtra filial', 5, 'O filtro voltou a funcionar e a equipe confirmou o resultado.'
    UNION ALL SELECT 'Solicitação de usuário somente para consulta', 5, 'Perfil criado conforme solicitado e sem permissões extras.'
    UNION ALL SELECT 'Acesso ao painel de métricas do produto', 5, 'Acesso liberado no mesmo dia.'
    UNION ALL SELECT 'Vazamento leve próximo ao bebedouro do segundo andar', 5, 'Atendimento rápido e área liberada com segurança.'
    UNION ALL SELECT 'Divergência no valor de auxílio mobilidade', 4, 'A equipe explicou o cálculo e ajustou o cadastro.'
    UNION ALL SELECT 'Troca de teclado com teclas intermitentes', 4, 'Equipamento substituído sem interromper o atendimento.'
    UNION ALL SELECT 'Solicitação de ajuste no acesso ao sistema de ponto', 5, 'O acesso foi corrigido e validado.'
) dados
JOIN chamados ch ON ch.empresa_id = @empresa_id AND ch.titulo = dados.titulo AND ch.status = 'RESOLVIDO'
WHERE NOT EXISTS (SELECT 1 FROM avaliacoes a WHERE a.chamado_id = ch.id);

-- Artigos para a base de conhecimento.
INSERT INTO artigos_faq (empresa_id, autor_id, categoria_id, titulo, conteudo, data_criacao, excluido)
SELECT @empresa_id, u.id, c.id, dados.titulo, dados.conteudo, DATE_SUB(NOW(), INTERVAL dados.dias DAY), b'0'
FROM (
    SELECT 'Como solicitar acessos no onboarding' AS titulo,
           'Abra um chamado em TI Interna - Onboarding com o cargo, a data de início e os sistemas necessários (e-mail, GitHub, AWS, VPN e ferramentas internas). Informe também quem é o gestor responsável pela solicitação.' AS conteudo,
           'TI Interna - Infraestrutura e Acessos' AS categoria, 'marina.costa@vertice.demo' AS autor, 20 AS dias
    UNION ALL SELECT 'Acesso à VPN e autenticação multifator',
           'Instale o autenticador corporativo e abra chamado em TI Interna - Infraestrutura e Acessos com a justificativa do acesso. Nunca compartilhe códigos de autenticação ou senha.',
           'TI Interna - Infraestrutura e Acessos', 'marina.costa@vertice.demo', 16
    UNION ALL SELECT 'Coparticipação e demonstrativo do plano de saúde',
           'A coparticipação é apresentada no demonstrativo de benefícios após o processamento da operadora. Para contestar um lançamento, abra chamado em Gente & Gestão - Benefícios e informe competência e procedimento, sem incluir dados médicos desnecessários.',
           'Gente & Gestão - Benefícios', 'rafael.nunes@vertice.demo', 12
    UNION ALL SELECT 'Como reportar problemas de infraestrutura do escritório',
           'Use a fila Facilities - Escritório. Informe sala ou posto, o problema observado, o impacto e o melhor horário para vistoria. Em caso de risco elétrico ou vazamento, sinalize a área e avise a recepção.',
           'Facilities - Escritório', 'camila.rocha@vertice.demo', 9
    UNION ALL SELECT 'Importação de retorno bancário no ERP Cloud',
           'Confira o banco, o layout CNAB, o período e a conta de liquidação. Se a importação falhar, anexe o arquivo de retorno com dados sensíveis mascarados e abra chamado em ERP Cloud - Integrações.',
           'ERP Cloud - Integrações', 'marina.costa@vertice.demo', 6
    UNION ALL SELECT 'Solicitação de reembolso corporativo',
           'Envie o chamado para Financeiro Interno com o motivo, a data e os comprovantes. Não inclua dados completos de cartão.',
           'Financeiro Interno', 'rafael.nunes@vertice.demo', 3
    UNION ALL SELECT 'Como recuperar o acesso ao ERP Cloud',
           'Na tela de acesso, use a opção de recuperação de senha e informe o e-mail cadastrado. Se a mensagem não chegar, verifique o spam e abra chamado em ERP Cloud - Suporte com o horário da tentativa.',
           'ERP Cloud - Suporte', 'camila.rocha@vertice.demo', 14
    UNION ALL SELECT 'Boas práticas para anexar arquivos em chamados',
           'Anexe somente arquivos necessários para reproduzir o problema. Remova senhas, tokens, dados pessoais e informações financeiras antes do envio. Descreva no chamado o resultado esperado e os passos realizados.',
           'ERP Cloud - Suporte', 'marina.costa@vertice.demo', 11
    UNION ALL SELECT 'Como solicitar manutenção de equipamento',
           'Abra chamado em TI Interna - Equipamentos, informe o comportamento observado, quando começou e se ocorre em outra estação. Não tente abrir o equipamento por conta própria.',
           'TI Interna - Equipamentos', 'camila.rocha@vertice.demo', 8
    UNION ALL SELECT 'Como acompanhar despesas e reembolsos',
           'Acompanhe solicitações no portal financeiro. Para pedir ajuda, use Financeiro Interno e informe a data de envio e o tipo de despesa. Não envie dados completos de cartão.',
           'Financeiro Interno', 'rafael.nunes@vertice.demo', 5
    UNION ALL SELECT 'Checklist para investigar integração com ERP',
           'Informe o módulo, ambiente, horário da falha, identificador da operação e mensagem de erro. Mascarar dados de clientes e credenciais antes de anexar logs ou arquivos.',
           'ERP Cloud - Integrações', 'marina.costa@vertice.demo', 2
) dados
JOIN usuarios u ON u.empresa_id = @empresa_id AND u.email = dados.autor
JOIN categorias c ON c.empresa_id = @empresa_id AND c.nome = dados.categoria
WHERE NOT EXISTS (
    SELECT 1 FROM artigos_faq a
    WHERE a.empresa_id = @empresa_id AND a.titulo = dados.titulo
);

-- Registros de auditoria de demonstração para a tela de histórico.
INSERT INTO logs_auditoria (empresa_id, usuario_id, nome_usuario, acao, entidade, entidade_id, detalhes, data_criacao)
SELECT @empresa_id, u.id, u.nome, 'DADOS_DEMONSTRATIVOS', 'chamados', ch.id,
       CONCAT('Registro fictício da demonstração Vértice Sistemas: ', ch.titulo),
       DATE_ADD(ch.data_abertura, INTERVAL 1 MINUTE)
FROM chamados ch
JOIN usuarios u ON u.empresa_id = @empresa_id AND u.email = 'marina.costa@vertice.demo'
WHERE ch.empresa_id = @empresa_id
  AND ch.titulo IN (
      'Onboarding: acessos e notebook para Desenvolvedor Pleno',
      'Ar-condicionado da Sala de Reunião 03 pingando',
      'Falha ao importar arquivo de retorno bancário',
      'Erro 502 no portal de pedidos'
  )
  AND NOT EXISTS (
      SELECT 1 FROM logs_auditoria l
      WHERE l.empresa_id = @empresa_id AND l.entidade = 'chamados'
        AND l.entidade_id = ch.id AND l.acao = 'DADOS_DEMONSTRATIVOS'
  );

COMMIT;

-- Resumo para conferência depois da execução.
SELECT 'usuarios' AS entidade, COUNT(*) AS quantidade FROM usuarios WHERE empresa_id = @empresa_id AND excluido = b'0'
UNION ALL SELECT 'categorias', COUNT(*) FROM categorias WHERE empresa_id = @empresa_id AND ativa = b'1'
UNION ALL SELECT 'chamados', COUNT(*) FROM chamados WHERE empresa_id = @empresa_id AND excluido = b'0'
UNION ALL SELECT 'interacoes', COUNT(*) FROM interacoes_chamado i JOIN chamados c ON c.id = i.chamado_id WHERE c.empresa_id = @empresa_id
UNION ALL SELECT 'avaliacoes', COUNT(*) FROM avaliacoes a JOIN chamados c ON c.id = a.chamado_id WHERE c.empresa_id = @empresa_id
UNION ALL SELECT 'artigos_faq', COUNT(*) FROM artigos_faq WHERE empresa_id = @empresa_id AND excluido = b'0';
