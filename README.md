# LazyDeploy — Backend

Backend da plataforma LazyDeploy, uma aplicação que monitora servidores de
Battlefield 4 e envia notificações quando as condições configuradas pelo
usuário são atendidas.

O projeto demonstra uma API REST segura, integrações com serviços externos,
monitoramento agendado, persistência relacional, autenticação por sessão e um
fluxo completo de notificações.

## Visão geral

O backend é responsável por:

- Autenticar usuários e manter a sessão com cookie HTTP;
- Cadastrar subscriptions de servidores monitorados;
- Consultar servidores e mapas nos providers externos;
- Avaliar regras de mapa e quantidade mínima de jogadores;
- Detectar mudanças de mapa;
- Enviar notificações por email através do Mailjet;
- Persistir tentativas e resultados no histórico;
- Expor endpoints de saúde, métricas e observabilidade;
- Aplicar limites de segurança e de uso da API.

A busca de servidores utiliza os providers GameTools e Battlelog Keeper configurados em `integration`.

## Demonstração e repositórios relacionados

- Aplicação: [lazydeploy.pages.dev](https://lazydeploy.pages.dev)
- Frontend: [github.com/yurepires/lazydeploy-frontend](https://github.com/yurepires/lazydeploy-frontend)
- Backend: [github.com/yurepires/lazydeploy-backend](https://github.com/yurepires/lazydeploy-backend)

## Stack

| Categoria | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 |
| API HTTP | Spring MVC |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | PostgreSQL |
| Migrations | Flyway |
| Segurança | Spring Security, sessão HTTP e CSRF |
| Integrações | GameTools, Battlelog Keeper e Mailjet |
| Mapeamentos | MapStruct |
| Rate limiting | Bucket4j + Caffeine |
| Build | Maven |
| Empacotamento | Docker multi-stage |

## Arquitetura

```text
HTTP Controller
      |
      v
Application Service
      |
      +--> Repository / PostgreSQL
      +--> Integrações externas
      |      +--> GameTools
      |      +--> Battlelog Keeper
      |      +--> Mailjet
      |
      +--> Scheduler de monitoramento
             |
             +--> Snapshot dos servidores
             +--> Avaliação das regras
             +--> Orquestração de notificações
             +--> Histórico de tentativas
```

Cada ciclo de monitoramento obtém um snapshot dos servidores, localiza as
subscriptions ativas, compara o estado anterior e cria uma notificação apenas
quando uma mudança atende às regras configuradas.

## Pré-requisitos

- Java 21;
- Docker e Docker Compose;
- Uma conta no Mailjet para envio de emails;
- PostgreSQL local ou uma instância PostgreSQL acessível pela aplicação.

O Maven Wrapper já está incluído no repositório. Não é necessário instalar o
Maven separadamente.

## Configuração local

### 1. Criar o ambiente do banco

Copie o arquivo de exemplo e altere os valores locais:

```powershell
Copy-Item .env.example .env
```

O arquivo `.env` é utilizado pelo Docker Compose para iniciar o PostgreSQL e
não deve ser commitado.

### 2. Iniciar o PostgreSQL

```powershell
docker compose up -d
docker compose ps
```

O Compose sobe somente o banco de dados. Para abrir o `psql` dentro do container:

```powershell
docker exec -it lazydeploy-postgres psql -U lazydeploy -d lazydeploy
```

### 3. Definir as variáveis da aplicação

No PowerShell, as variáveis mínimas para executar localmente são:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/lazydeploy"
$env:DATABASE_USERNAME = "lazydeploy"
$env:DATABASE_PASSWORD = "change-me"
$env:SPRING_PROFILES_ACTIVE = "default"
```

O `.env` do Docker Compose não é carregado automaticamente pelo IntelliJ ou
pelo Spring Boot. Na IDE, configure essas variáveis na Run Configuration.

### 4. Iniciar a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

Ao iniciar, o Flyway executa as migrations pendentes e o Hibernate valida o
schema existente. A aplicação fica disponível em `http://localhost:8080`.

## Variáveis de ambiente

### Banco e servidor

| Variável | Obrigatória | Finalidade |
| --- | --- | --- |
| `DATABASE_URL` | Sim | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` | Sim | Usuário do PostgreSQL |
| `DATABASE_PASSWORD` | Sim | Senha do PostgreSQL |
| `PORT` | Não | Porta HTTP; padrão `8080` |
| `SPRING_PROFILES_ACTIVE` | Não | Use `prod` em produção |
| `LAZYDEPLOY_FRONTEND_ORIGIN` | Em produção | Origem exata liberada pelo CORS |

### Mailjet

| Variável | Obrigatória | Finalidade |
| --- | --- | --- |
| `MAILJET_API_KEY` | Em produção | Chave pública da API |
| `MAILJET_API_SECRET` | Em produção | Chave secreta da API |
| `MAILJET_FROM_EMAIL` | Em produção | Remetente validado no Mailjet |
| `MAILJET_FROM_NAME` | Não | Nome exibido no remetente |
| `MAILJET_BASE_URL` | Não | URL base da API |
| `MAILJET_CONNECT_TIMEOUT_MS` | Não | Timeout de conexão |
| `MAILJET_RESPONSE_TIMEOUT_MS` | Não | Timeout de resposta |
| `MAILJET_MAX_RESPONSE_BODY_BYTES` | Não | Limite do corpo retornado |

`MAILJET_FROM_EMAIL` deve ser exatamente um remetente autorizado no Mailjet.
O destinatário é o email da conta proprietária da subscription.

Limites de banco, executor, Tomcat, rate limiting e regras de negócio podem ser
ajustados no `application.yaml` e pelas variáveis documentadas em
`.env.example`.

## Execução com Docker

O `Dockerfile` usa dois estágios: uma imagem Maven com Java 21 para compilar o
JAR e uma imagem menor com apenas o runtime Java e a aplicação. O processo
final executa com o usuário não-root `lazydeploy` e expõe a porta 8080.

```powershell
docker build -t lazydeploy-backend:local .
```

Para executar o backend junto do banco local, use o nome do serviço Docker,
`postgres`, como host:

```powershell
docker compose up -d

docker run --rm --name lazydeploy-backend `
  --network lazy-deploy_default `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/lazydeploy `
  -e DATABASE_USERNAME=lazydeploy `
  -e DATABASE_PASSWORD=change-me `
  -e LAZYDEPLOY_FRONTEND_ORIGIN=https://lazydeploy.pages.dev `
  -e MAILJET_API_KEY=change-me `
  -e MAILJET_API_SECRET=change-me `
  -e MAILJET_FROM_EMAIL=mailer@example.com `
  -e MAILJET_FROM_NAME=LazyDeploy `
  -e PORT=8080 `
  lazydeploy-backend:local
```

## Autenticação e segurança

A autenticação usa sessão HTTP no servidor:

- `JSESSIONID` identifica a sessão autenticada e é `HttpOnly`;
- `XSRF-TOKEN` é disponibilizado para o frontend;
- operações mutáveis exigem o header `X-XSRF-TOKEN`;
- endpoints de negócio identificam o usuário pela sessão, nunca por um
  `userId` enviado pelo cliente;
- em produção, cookies usam `Secure` e `SameSite=None` para frontend e backend
  em origens diferentes.

Endpoints públicos:

```text
GET  /api/auth/csrf
POST /api/auth/register
POST /api/auth/login
```

Endpoints que exigem sessão:

```text
POST /api/auth/logout
GET  /api/auth/me
GET  /api/bf4/**
```

A aplicação também aplica rate limiting por IP, email normalizado e usuário,
limite de 1 MiB para corpos JSON, limites de negócio, headers de segurança,
CSP, HSTS em produção, CORS restrito e tratamento global de exceções com
respostas RFC 9457 (`ProblemDetail`).

Em produção, apenas health, liveness e readiness do Actuator são expostos.
`info` e `metrics`, quando habilitados no ambiente de desenvolvimento, exigem
a role `ADMIN`.

## API principal

### Servidores, mapas e notificações

```text
GET /api/bf4/servers/search?query=<nome>&limit=20
GET /api/bf4/maps
GET /api/bf4/maps/{mapId}
GET /api/bf4/notifications
GET /api/bf4/notifications/{notificationId}
GET /api/bf4/subscriptions/{id}/notifications
```

### Subscriptions

```text
POST   /api/bf4/subscriptions
GET    /api/bf4/subscriptions
GET    /api/bf4/subscriptions/{id}
PUT    /api/bf4/subscriptions/{id}
PATCH  /api/bf4/subscriptions/{id}
DELETE /api/bf4/subscriptions/{id}
```

### Regras

```text
GET    /api/bf4/subscriptions/{id}/rules
POST   /api/bf4/subscriptions/{id}/rules
GET    /api/bf4/subscriptions/{id}/rules/{ruleId}
PUT    /api/bf4/subscriptions/{id}/rules/{ruleId}
PATCH  /api/bf4/subscriptions/{id}/rules/{ruleId}
DELETE /api/bf4/subscriptions/{id}/rules/{ruleId}
```

Tipos disponíveis:

- `MAP_IN`: mapa atual entre os mapas configurados;
- `PLAYER_COUNT_AT_LEAST`: quantidade de jogadores igual ou superior ao mínimo.

### Canais de notificação

```text
GET    /api/bf4/subscriptions/{id}/channels
POST   /api/bf4/subscriptions/{id}/channels
GET    /api/bf4/subscriptions/{id}/channels/{channelId}
PUT    /api/bf4/subscriptions/{id}/channels/{channelId}
PATCH  /api/bf4/subscriptions/{id}/channels/{channelId}
DELETE /api/bf4/subscriptions/{id}/channels/{channelId}
```

O canal disponível atualmente é `EMAIL`. Ao criar ou atualizar um canal, envie
apenas `type` e `enabled`; o destinatário é o email da conta autenticada. A
estrutura foi mantida para permitir novos canais no futuro.

### Histórico

`GET /api/bf4/notifications` aceita `page`, `size`, `status`, `channel`,
`mapId`, `serverId`, `subscriptionId`, `from` e `to`.

- `page` começa em `0`;
- `size` usa 20 por padrão e aceita no máximo 100;
- a ordenação padrão é `attemptedAt` decrescente;
- também são aceitos `sentAt`, `status` e `channelType`;
- o snapshot do servidor, mapa e jogadores é preservado;
- tentativas permanecem disponíveis após a remoção da subscription.

Erros seguem `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Descrição segura do problema",
  "errorCode": "VALIDATION_FAILED",
  "timestamp": "2026-01-01T00:00:00Z",
  "fieldErrors": []
}
```

## Observabilidade

Em desenvolvimento:

```text
GET /actuator/health
GET /actuator/info       (ADMIN)
GET /actuator/metrics    (ADMIN)
```

Em produção, `prod` expõe apenas health, liveness e readiness. Os checks não
fazem chamadas aos providers externos nem ao Mailjet. As métricas internas
acompanham requisições, duração, timeouts, rejeições por concorrência, retries,
ciclos ignorados e rejeições dos executores.

## Migrations

As migrations ficam em `src/main/resources/db/migration` e são executadas pelo
Flyway no startup. O Hibernate opera com `ddl-auto=validate`, portanto não cria
nem altera tabelas automaticamente.

## Testes e qualidade

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

A suíte cobre autenticação, autorização, CSRF, rate limiting, validação, regras
de notificação, histórico, integrações e limites de segurança.

## Estrutura do código

```text
src/main/java/com/yurepires/lazydeploy
├── config         # propriedades e beans
├── controller     # endpoints HTTP
├── dto            # requests e responses
├── entity         # entidades JPA
├── exception      # exceções e handler global
├── integration    # GameTools, Keeper e Mailjet
├── mapper         # mapeamentos MapStruct
├── model          # contratos do domínio
├── repository     # interfaces JpaRepository
├── security       # sessão, CSRF, CORS e autorização
└── service        # regras e monitoramento
```

Controllers coordenam a entrada HTTP, services aplicam as regras, repositories
persistem dados e mappers isolam conversões entre DTOs e entidades.

## Como contribuir

Contribuições são bem-vindas para correções, melhorias de segurança,
documentação e novas funcionalidades.

1. Faça um fork do repositório e crie uma branch específica para a alteração:
   `feature/nome-da-funcionalidade` ou `fix/nome-do-problema`.
2. Mantenha cada commit focado em uma mudança e use mensagens descritivas.
3. Atualize ou crie testes para o comportamento alterado.
4. Execute a suíte localmente antes de abrir o Pull Request:

   ```powershell
   .\mvnw.cmd test
   ```

5. Descreva no Pull Request o problema resolvido, as decisões relevantes e
   como validar a alteração.

Ao contribuir, preserve a separação de responsabilidades do projeto: regras de
negócio devem permanecer nos services, acesso a dados nos repositories e
comunicação externa nos adapters de `integration`. Alterações de banco devem
usar uma nova migration Flyway, sem modificar migrations já aplicadas.

Não envie credenciais, arquivos `.env`, chaves de API, dados de produção ou
informações pessoais. Para vulnerabilidades, por favor, entre em contato comigo.

## Licença

Este repositório ainda não possui um arquivo de licença (`LICENSE`). Portanto,
o código permanece protegido pelos direitos autorais aplicáveis e não há uma
autorização automática para uso comercial, redistribuição, sublicenciamento ou
criação de versões derivadas além do permitido pela legislação.

Você pode consultar o código publicamente para fins de avaliação do portfólio,
mas deve obter autorização do autor antes de redistribuí-lo ou incorporá-lo em
outro projeto. Caso o projeto seja liberado como open source no futuro, um
arquivo `LICENSE` será adicionado ao repositório e esta seção será atualizada
com os termos escolhidos.
