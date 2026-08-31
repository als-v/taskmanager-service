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


## Build

Scripts principais:

Arquivos Compose:

- `docker-compose.yml`: ferramentas `postgres`, `redis`, `pgadmin`, `sonarqube`, `jenkins`.
- `docker-compose.app.yml`: aplicacao usando a imagem `taskmanager-api:local`.
- `docker-compose.build.yml`: instrucoes de build das imagens locais (`api` e `jenkins`).

Bootstrap de infraestrutura local:

```bash
docker compose -f docker-compose.yml -f docker-compose.build.yml build jenkins
docker compose up -d postgres redis pgadmin sonarqube jenkins
```

- `deploy.sh` usa os dois arquivos para buildar as imagens locais e subir as dependencias: `postgres`, `redis`, `pgadmin`, `sonarqube`, inclusindo `api` e `jenkins`.
- `seed-db.sh` aplica seed.
- `reset-env.sh` reseta o ambiente.

Portas padrao: API `8080`, pgAdmin `5050`, SonarQube `9000`, Jenkins `8081`.

Usuarios seed: `usuario1@usuario1.com` ate `usuario10@usuario10.com`, todos com senha `123mudarA@`. O seed cria 20 projetos, 1000 tasks e dados de audit log/notificacoes, respeitando WIP maximo de 5 `IN_PROGRESS` por responsavel dentro do projeto.