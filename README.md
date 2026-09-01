# Task Manager Service

Servico gerenciamento de tarfas.

## Como rodar

Requisitos: **Java 17+** e **Docker**.

**Testes**:

```bash
./mvnw test
```

**Aplicacao completa:**

```bash
cp .env.example .env
docker compose up -d

set -a; source .env; set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Todos os serviços:**
- postgres16
- redis
- pgAdmin (opcional)
- sonarcube (opcional)
- jenkins (opcional)
- loki
- promtail
- grafana

Portas padrao: API `8080`, pgAdmin `5050`, SonarQube `9000`, Jenkins `8081`, Grafana `3000`.


**Scrips:**

```bash
./scripts/deploy.sh           # deploy
./scripts/seed-db.sh          # seed com 20 projetos, 1000 tasks
./scripts/reset-env.sh        # reseta o ambiente
```

A execução pode ser feita através dos scripts isoladamente, ou através do jenkins.

**Importante:**

Usuarios seed: `usuario1@usuario1.com` ate `usuario10@usuario10.com`, todos com senha `123mudarA@`. O seed cria 20 projetos, 1000 tasks e dados de audit log/notificacoes, respeitando WIP maximo de 5 `IN_PROGRESS` por responsavel dentro do projeto.

## Organização

O projeto está organizado da seguinte maneira:


```
controller/         # camada de entrada HTTP
service/            # regra de negócio
policy/             # politica
repository/         # jpa
domain/             # camada dominio da aplicacao
  ├─ entity/        # entidades
  ├─ dto/           # dtos
  ├─ enumeration/   # enums
  └─ error/         # errors
security/           # filtros e config jwt
logging/            # logs
pagination/         # paginacao generica
config/             # camada configuracoes
```

## Arquitetura

No diretorio `/docs` é disponibilizado alguns documentos importantes:

-  `docs/db`: conta com a arquitetura do sistema;
-  `docs/diagrams`: conta com alguns diagramas de sequencia dos fluxos considerados sensiveis da aplicação;
-  `docs/routes`: conta com a exportação da coleção do postman com as rotas;

## Observabilidade

Além dos logs de auditoria, foi adicionado uma camada de observabilidade da aplicação.

O que é registrado hoje:

- Autenticação com sucesso/falha e registro;
- Toda ação registrada nos logs de auditoria;
- Logs operacionais da aplicação;

Essa camada não substitui os logs da aplicação, mas os complementa. Com ela é possivel ter uma visão analitica da aplicação como um todo.

## Cache Redis
 
Para estratégia de cache, está sendo utilizado Spring Cache + Redis em runtime e cache simples para testes não terem dependencia redis.

Caches configurados:

- `dashboard:summary` - TTL de 5 minutos: chave por usuario autenticado e `projectId` selecionado ou `null` para geral.
- `dashboard:wip` - TTL de 2 minutos: chave por usuario autenticado e `projectId` selecionado ou `null` para geral.
- `project:members:list` - TTL de 5 minutos: chave por usuario autenticado, `projectId`, pagina, tamanho, nome e email.
- `user:projects:list` - TTL de 2 minutos: chave por usuario autenticado, pagina, tamanho, ordenacao, nome e descricao.

A invalidacao acontece nos inserts: Insert de task limpa dashboard e WIP; Insert de project limpa projetos do usuario e dashboards; Adicao de membros limpa membros do projeto, projetos do usuario e dashboards.

Tasks, Notifications e AuditLogs não serão cacheados por serem dados sensíveis que possuem alteracoes mais frequentes.

## Melhorias

**H2 para testes** Escolha inicial pela velocidade para implementação dos testes, mas que por conta de problemas de compatibilidade entre h2 e postgres, o melhor seria migrar para ele também.

**Autenticacao** Falta implementar reset de senha, controle de sessão, banimento por IP, e rotacionamento de refresh token, que hoje é controlado via redis.

**Cache** Ainda há mais métodos que poderiam estar cacheados com estratégias especificas, como o gerenciamento das tasks. Atualmente foi decidido nao segui com o cachemaneto das tasks, notificacoes e audit logs por serem muito voláteis, o custo do usuario ficar com informações desatualizadas é muito maior que custo de cache.

**WIP** Atualmente é apenas um `COUNT`, o ideal seria armazenar em um contador unico para cada usuario + projeto. Porém, no momento atual traria alguns problemas de condiçoes de corrida, na qual a complexidade nao se justificaria. Além do que enquanto o limite for baixo (eg, 5) o custo é infimo.

**Testes** Necessário mais testes completos (utilizandoSpringBootTest ), atualmente a maioria dos testes desenvolvidos sao testes unitários

**Deploy** O deploy atualmente está simples porém mais facil de inspecionar. Seria importante pegar a base do que ja tem e evoluir para clusterização, para controle e gerenciamento mais especifico da aplicação. Também, deploy nao tem rollback, gerenciamento de versões, etc, todos esses detalhes devem ser levados em consideração.

**Logs** Dashboard no grafana está muito simples ainda, precisa de um refino. Também referente aos logs, o ideal seria analisar pontos chaves da aplicação para manter uma rastreabilidade de que é critico. Além dos logs, as metricas também seriam importantes, atualmente nao são coletadas.

**Arquitetura** Consigo ver algumas melhorias referente a arquitetura da solução, como não deixar: `TODO`/`IN_PROGRESS`/`DONE` fixos, mas sim genéricos e configuraveis. Podemos modelar esses types como funis configuravel por projetos, passando de sempre ter 3 para ter n.

**Comunicação Assicrona** Hoje caso mais de um usuario esteja utilizando um mesmo projeto ao mesmo tempo, as alterações não irao ser replicadas a ambos. A utilização de ws/sse nesses casos iriam manter ambos atualizados com as mesmas informações. Também, com essas tecnologias nao seria mais necessário o pooling das notificações.