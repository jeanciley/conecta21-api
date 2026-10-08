# Módulos opcionais e GMUD

Administradores podem abrir **Módulos** na navegação para ativar recursos opcionais para o tenant atual. A tela consome um catálogo compartilhado; novos recursos podem ser adicionados ao catálogo do backend sem criar uma coluna específica em `empresas`.

Execute `migration_007_modulos.sql` após a migração `migration_006_gmud.sql` em bancos existentes. A migração cria `empresa_modulos`, conserva contratos GMUD já ativos e remove a antiga coluna `empresas.gmud_ativo`. Bancos novos podem usar `script.sql`, que já inclui a estrutura genérica e começa sem módulos contratados.

A GMUD permanece disponível apenas para técnicos e administradores dos tenants que a contrataram. A API valida perfil, tenant e contrato em todas as operações GMUD. A planilha do protótipo está incorporada como modelo de geração; responsáveis vêm da tabela `usuarios`, e o arquivo e as evidências ficam no storage e como anexos do chamado.
