# Task Manager Service

Servico gerenciamento de tarfas.

## Cache Redis
 
Para estratégia de cache, está sendo utilizado Spring Cache + Redis em runtime e cache simples para testes não terem dependencia redis.

Caches configurados:

- `dashboard:summary` - TTL de 5 minutos: chave por usuario autenticado e `projectId` selecionado ou `null` para geral.
- `dashboard:wip` - TTL de 2 minutos: chave por usuario autenticado e `projectId` selecionado ou `null` para geral.
- `project:members:list` - TTL de 5 minutos: chave por usuario autenticado, `projectId`, pagina, tamanho, nome e email.
- `user:projects:list` - TTL de 2 minutos: chave por usuario autenticado, pagina, tamanho, ordenacao, nome e descricao.

A invalidacao acontece nos inserts: Insert de task limpa dashboard e WIP; Insert de project limpa projetos do usuario e dashboards; Adicao de membros limpa membros do projeto, projetos do usuario e dashboards.

Tasks, Notifications e AuditLogs não serão cacheados por serem dados sensíveis que possuem alteracoes mais frequentes.
