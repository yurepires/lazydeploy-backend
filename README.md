# LazyDeploy

## Execução com Docker

O `Dockerfile` usa dois estágios: o primeiro compila o JAR com Java 21 e o
segundo contém somente o runtime Java e o JAR da aplicação. O processo final
executa com o usuário não-root `lazydeploy`.

Para criar a imagem local:

```shell
docker build -t lazydeploy-backend:local .
```

Para executar a imagem usando o perfil de produção, suba primeiro o PostgreSQL
local e passe as variáveis por fora da imagem. O arquivo `.env` local é
ignorado pelo Git e não é enviado ao contexto do build:

```shell
docker compose up -d

docker run --rm --name lazydeploy-backend \
  --network lazy-deploy_default \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/lazydeploy \
  -e DATABASE_USERNAME=lazydeploy \
  -e DATABASE_PASSWORD=change-me \
  -e LAZYDEPLOY_FRONTEND_ORIGIN=https://lazydeploy.pages.dev \
  -e MAIL_HOST=smtp.example.com \
  -e MAIL_PORT=587 \
  -e MAIL_USERNAME=mailer@example.com \
  -e MAIL_PASSWORD=change-me \
  -e MAIL_FROM=mailer@example.com \
  -e PORT=8080 \
  lazydeploy-backend:local
```

Em uma rede Docker compartilhada, use o nome do serviço (`postgres`) como host
do PostgreSQL. O container não recebe código-fonte, Maven, `.git` ou arquivos
`.env`; as migrations do Flyway continuam sendo executadas no startup.

Variáveis obrigatórias do perfil `prod`:

| Variável | Finalidade |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | Ativa a configuração de produção |
| `DATABASE_URL` | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` | Usuário do PostgreSQL |
| `DATABASE_PASSWORD` | Senha do PostgreSQL |
| `LAZYDEPLOY_FRONTEND_ORIGIN` | Origem exata permitida pelo CORS |
| `MAIL_HOST` | Host SMTP |
| `MAIL_PORT` | Porta SMTP |
| `MAIL_USERNAME` | Usuário SMTP |
| `MAIL_PASSWORD` | Senha SMTP |
| `MAIL_FROM` | Remetente das notificações |

`PORT` é opcional e usa `8080` quando não for fornecida. Nenhuma dessas
credenciais deve ser colocada no `Dockerfile`, no repositório ou em argumentos
de build.

## Banco de dados local

O PostgreSQL roda no Docker; não é necessário instalar PostgreSQL, `psql` ou pgAdmin no Windows.

1. Copie `.env.example` para `.env` e altere a senha.
2. Suba o banco:

   ```shell
   docker compose up -d
   ```

3. Exporte `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` com os mesmos valores do `.env`.
4. Inicie a aplicação. O Flyway aplicará as migrations e o Hibernate validará o schema.

Para inspecionar o banco pelo próprio container:

```shell
docker exec -it lazydeploy-postgres psql -U lazydeploy -d lazydeploy
```

O volume `lazydeploy_postgres_data` mantém os dados após reinícios do container.

## Autenticação

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/auth/csrf`

O cadastro e o login utilizam email e senha. A aplicação normaliza o email,
armazena somente o hash da senha e mantém a identidade autenticada em uma sessão
HTTP no servidor. Os endpoints de negócio obtêm o usuário da sessão atual; não
aceitam mais `X-User-Id` ou `userId` enviado pelo cliente.

Em desenvolvimento, a sessão usa cookie `HttpOnly` com `SameSite=Lax`. Em
produção, ative o perfil `prod`; ele configura `JSESSIONID` como `HttpOnly`,
`Secure`, `SameSite=None` e com path `/`, sem definir um domínio compartilhado.

A aplicação também aplica rate limiting em memória antes dos endpoints de
autenticação e de busca/configuração de subscriptions. Os limites padrão ficam
em `lazydeploy.security.rate-limit` no `application.yaml`: login por IP e por
email normalizado, cadastro por IP, busca por usuário/IP e configuração por
usuário/IP. O cache possui tamanho máximo e expiração por inatividade; em uma
implantação com várias instâncias será necessário trocar esse armazenamento por
um backend compartilhado.

Quando um limite é atingido, a API responde HTTP 429 com `Content-Type:
application/problem+json` e o header `Retry-After`, sem indicar se o bloqueio
foi causado pelo IP ou pelo email.

Os limites de negócio ficam em `lazydeploy.limits`: por padrão, cada usuário
possui até 20 subscriptions, cada alerta até 10 regras e 5 canais, e uma regra
`MAP_IN` aceita até 20 mapas. Consultas de servidores aceitam entre 2 e 100
caracteres. O histórico usa páginas de até 100 itens e rejeita páginas acima de
10.000. Corpos JSON da API são limitados a 1 MiB. Esses valores podem ser
ajustados no arquivo de configuração sem recompilar a aplicação.

Por padrão, cabeçalhos `X-Forwarded-For` são ignorados. Só habilite
`lazydeploy.security.rate-limit.proxy.trust-forwarded-headers` quando a
aplicação estiver atrás de um proxy conhecido e preencha
`trusted-proxies` com os endereços desse proxy.

As integrações externas usam políticas independentes em
`lazydeploy.providers`. Cada provider possui timeout de conexão, timeout de
resposta, limite de chamadas simultâneas e uma política de retry. O GameTools
não faz retry para manter a busca interativa rápida; o Keeper pode fazer uma
segunda tentativa apenas em timeout, falha de conexão ou erro 5xx. Quando o
limite de concorrência do GameTools é atingido, a API responde HTTP 503 com
`EXTERNAL_PROVIDER_BUSY`; as demais falhas externas usam
`EXTERNAL_PROVIDER_UNAVAILABLE`.

As métricas agregadas dos providers ficam disponíveis pelo Actuator em
`lazydeploy.provider.requests`, `lazydeploy.provider.duration`,
`lazydeploy.provider.timeouts`, `lazydeploy.provider.concurrency_rejections` e
`lazydeploy.provider.retries`, sempre com tags de provider e resultado de baixa
cardinalidade.

Os recursos internos também possuem limites explícitos em
`lazydeploy.resources`: o pool Hikari usa tamanho máximo, mínimo de conexões
ociosas e timeouts finitos; o executor do monitoramento tem pool e fila
limitados; e o scheduler executa um único trigger por vez. O ciclo de
monitoramento usa uma política single-flight, portanto um ciclo lento não gera
uma fila ilimitada de novos ciclos. Rejeições e ciclos ignorados são expostos
em `lazydeploy.executor.rejections` e
`lazydeploy.monitoring.cycles.skipped`.

O servidor Tomcat usa limites finitos para threads, conexões, fila de aceite e
timeout de conexão. O desligamento do Spring Boot é gracioso e aguarda apenas
o tempo configurado em `SHUTDOWN_TIMEOUT` (20 segundos por padrão). Os
timeouts SMTP também são finitos e podem ser ajustados pelas variáveis
`MAIL_SMTP_CONNECTION_TIMEOUT_MS`, `MAIL_SMTP_READ_TIMEOUT_MS` e
`MAIL_SMTP_WRITE_TIMEOUT_MS`.

Como a autenticação utiliza cookies, operações mutáveis exigem token CSRF. Para
testes manuais, faça primeiro `GET /api/auth/csrf`, envie o cookie recebido e
repita o valor no header `X-XSRF-TOKEN`.

## Actuator e health checks

Os endpoints de gerenciamento permanecem fora de `/api`, no namespace
`/actuator`. O perfil padrão de desenvolvimento expõe `health`, `info` e
`metrics`; `health` é público e `info`/`metrics` exigem um usuário com a role
`ADMIN`. Detalhes de health só aparecem para usuários autorizados.

Para executar em produção, ative explicitamente o perfil `prod` com
`SPRING_PROFILES_ACTIVE=prod`. Nesse perfil somente `GET /actuator/health` e os
grupos `liveness`/`readiness` ficam expostos; os detalhes e componentes são
ocultados, enquanto `info` e `metrics` deixam de ser endpoints web. O liveness
usa apenas o estado do processo e o readiness pode verificar o banco, mas
nenhum dos dois dispara chamadas ao Keeper, GameTools, BFLIST ou SMTP.

Endpoints administrativos sensíveis, como `env`, `configprops`, `beans`,
`mappings`, `heapdump`, `threaddump`, `loggers` e `shutdown`, não são expostos;
o endpoint de desligamento também está desabilitado explicitamente.

## Segurança HTTP para frontend cross-site

O perfil padrão permite somente `http://localhost:4200` na allowlist CORS. O
perfil `prod` exige `LAZYDEPLOY_FRONTEND_ORIGIN` e permite apenas essa origem
exata, sem curingas ou reflexão do header `Origin`. CORS aceita credenciais,
preflight e os métodos necessários para a API; `/actuator/**` não herda essa
configuração.

O endpoint `GET /api/auth/csrf` materializa o cookie `XSRF-TOKEN`. Ele é legível
pelo Angular, enquanto `JSESSIONID` permanece `HttpOnly`. Todas as operações
mutáveis continuam exigindo `X-XSRF-TOKEN`; uma falha retorna HTTP 403 com
`CSRF_VALIDATION_FAILED` e uma mensagem genérica. O token CSRF não autentica o
usuário e nunca substitui a sessão.

No perfil `prod`, o cookie CSRF também usa `Secure` e `SameSite=None`, necessários
para o cenário Cloudflare Pages + Railway. Isso depende do navegador aceitar
cookies cross-site; essa limitação deve ser validada em Chrome, Firefox, Edge e
Safari antes do deploy final.

As respostas recebem `X-Content-Type-Options`, `X-Frame-Options: DENY`,
`Referrer-Policy`, CSP restritiva para uma API, `Permissions-Policy` mínima e
`Cache-Control` sem armazenamento. HSTS é habilitado apenas para respostas
HTTPS do perfil `prod`, sem `includeSubDomains` ou `preload`.

## API BF4

- `GET /api/bf4/servers/search?query=<nome>&limit=20`
- `GET /api/bf4/maps`
- `GET /api/bf4/maps/{mapId}`
- `GET /api/bf4/notifications`
- `GET /api/bf4/notifications/{notificationId}`
- `GET /api/bf4/subscriptions/{id}/notifications`
- `POST /api/bf4/subscriptions`
- `GET /api/bf4/subscriptions`
- `GET /api/bf4/subscriptions/{id}`
- `PUT|PATCH /api/bf4/subscriptions/{id}`
- `DELETE /api/bf4/subscriptions/{id}`
- `GET|POST /api/bf4/subscriptions/{id}/rules`
- `GET|PUT|PATCH|DELETE /api/bf4/subscriptions/{id}/rules/{ruleId}`
- `GET|POST /api/bf4/subscriptions/{id}/channels`
- `GET|PUT|PATCH|DELETE /api/bf4/subscriptions/{id}/channels/{channelId}`

As respostas de erro seguem o formato RFC 9457 (`ProblemDetail`), com os campos
`errorCode`, `timestamp` e, quando aplicável, `fieldErrors`. Os tipos de regra
disponíveis são `MAP_IN` e `PLAYER_COUNT_AT_LEAST`; o canal disponível nesta fase
é `EMAIL`, sem parâmetros, cujo destino é sempre o endereço de email da conta
proprietária.
Ao criar ou atualizar esse canal, envie apenas `type` e `enabled`; qualquer
parâmetro, incluindo `recipient`, é rejeitado.

O catálogo de mapas retorna somente mapas habilitados e ordenados pelo nome
amigável. As regras `MAP_IN` continuam armazenando o identificador técnico (por
exemplo, `MP_Prison`); o catálogo é usado para validação e apresentação. Se o
Keeper enviar um mapa ainda não catalogado, o monitoramento continua e usa o
próprio identificador técnico como nome de exibição.

O histórico de notificações é somente leitura e sempre pertence ao usuário da
sessão atual. Os endpoints aceitam `page` (inicia em 0), `size` (padrão 20,
máximo 100), `status`, `channel`, `mapId`, `serverId`, `subscriptionId`, `from`
e `to`. A ordenação padrão é `attemptedAt` decrescente; também são aceitos
`sentAt`, `status` e `channelType`. O histórico mantém um snapshot do servidor,
mapa e jogadores no momento da tentativa, inclusive quando a entrega falha.
Os limites `from` e `to` são inclusivos.
Quando uma inscrição é removida, as tentativas permanecem preservadas; a
referência da inscrição pode ficar nula, mas o histórico continua vinculado ao
usuário que originou a tentativa.

Os endpoints `POST /api/auth/register`, `POST /api/auth/login` e
`GET /api/auth/csrf` são públicos. `POST /api/auth/logout` e `GET /api/auth/me`
exigem uma sessão autenticada. Todos os demais endpoints em `/api/bf4/**`
continuam exigindo autenticação.

## Estrutura do código

- `config`: propriedades e beans de configuração.
- `controller`: endpoints HTTP.
- `dto.request`: dados recebidos pela API.
- `dto.response`: respostas da API, incluindo erros.
- `entity`: entidades persistidas pelo JPA.
- `exception`: exceções da aplicação e tratamento global de erros.
- `integration`: comunicação com GameTools e Battlelog Keeper.
- `mapper`: conversões entre entidades JPA e modelos usando MapStruct.
- `model`: objetos imutáveis e contratos do domínio.
- `repository`: interfaces Spring Data que estendem `JpaRepository`.
- `service`: regras de aplicação, monitoramento e notificações.
