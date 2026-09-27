# Conecta21: perfis personalizados e chamados internos

Em bancos existentes, aplique `migration_004_categorias_perfis_internos.sql` depois das migrations 001 a 003 e antes de iniciar a API. A migration cria perfis por empresa, liga o perfil personalizado aos usuários, registra exclusão lógica e marca chamados internos. Faça backup antes da alteração. Para instalação nova, use `script.sql` atualizado.

As exclusões de usuário são lógicas (`ativo = false` e `excluido_em`), preservando o histórico de chamados. Um perfil personalizado só pode ser desativado quando nenhum usuário estiver vinculado a ele.

Os relatórios ficam disponíveis em `GET /api/relatorios/chamados/csv` e `GET /api/relatorios/chamados/pdf`. A API verifica a permissão `GERAR_RELATORIOS`; perfis sem acesso não recebem chamados internos no arquivo.
