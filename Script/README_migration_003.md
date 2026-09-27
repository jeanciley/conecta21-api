# Conecta21: prioridades, SLAs e ativação de contas

Para bancos já existentes, faça backup e aplique `migration_003_prioridades_sla_onboarding.sql` uma única vez antes de iniciar a API. O Hibernate continua em `ddl-auto: validate`, então não cria nem altera tabelas automaticamente.

Configure o envio de e-mail por variáveis de ambiente:

- `SMTP_HOST`, `SMTP_PORT`
- `SMTP_USERNAME`, `SMTP_PASSWORD`
- `SMTP_AUTH`, `SMTP_STARTTLS`
- `FRONTEND_URL`: URL base onde ficam `ativar-conta.html` e `redefinir-senha.html`

Em desenvolvimento, use um servidor SMTP local de captura de e-mails ou credenciais SMTP de teste. Novos membros recebem um link com validade de 30 minutos; redefinições de senha usam o mesmo prazo.

`script.sql` contém a estrutura para uma instalação nova. O cadastro de empresa cria a prioridade inicial “Padrão” e associa a ela as categorias iniciais.
