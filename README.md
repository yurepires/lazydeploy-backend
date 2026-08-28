# LazyDeploy

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

- `POST /api/bf4/auth/register`
- `POST /api/bf4/auth/login`
- `POST /api/bf4/auth/logout`
- `GET /api/bf4/auth/me`
- `GET /api/bf4/auth/csrf`

O cadastro e o login utilizam email e senha. A aplicação normaliza o email,
armazena somente o hash da senha e mantém a identidade autenticada em uma sessão
HTTP no servidor. Os endpoints de negócio obtêm o usuário da sessão atual; não
aceitam mais `X-User-Id` ou `userId` enviado pelo cliente.

Em produção, defina `SESSION_COOKIE_SECURE=true` quando a aplicação estiver
atrás de HTTPS.

Como a autenticação utiliza cookies, operações mutáveis exigem token CSRF. Para
testes manuais, faça primeiro `GET /api/bf4/auth/csrf`, envie o cookie recebido e
repita o valor no header `X-XSRF-TOKEN`.

## API BF4

- `GET /api/bf4/servers/search?query=<nome>&limit=20`
- `GET /api/bf4/maps`
- `GET /api/bf4/maps/{mapId}`
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
é `EMAIL`, configurado somente com o parâmetro `recipient`.

O catálogo de mapas retorna somente mapas habilitados e ordenados pelo nome
amigável. As regras `MAP_IN` continuam armazenando o identificador técnico (por
exemplo, `MP_Prison`); o catálogo é usado para validação e apresentação. Se o
Keeper enviar um mapa ainda não catalogado, o monitoramento continua e usa o
próprio identificador técnico como nome de exibição.

Todos os endpoints em `/api/bf4/**`, exceto cadastro, login e obtenção do token
CSRF, exigem autenticação. O logout exige uma sessão autenticada.

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

O acesso ao banco segue o fluxo `service -> repository -> entity`. Não existe uma
camada adicional de adapters. Quando o service precisa trabalhar com um modelo de
domínio, a conversão entre esse modelo e a entidade é feita por um mapper dedicado.
