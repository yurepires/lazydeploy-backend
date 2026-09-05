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

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/auth/csrf`

O cadastro e o login utilizam email e senha. A aplicação normaliza o email,
armazena somente o hash da senha e mantém a identidade autenticada em uma sessão
HTTP no servidor. Os endpoints de negócio obtêm o usuário da sessão atual; não
aceitam mais `X-User-Id` ou `userId` enviado pelo cliente.

Em produção, defina `SESSION_COOKIE_SECURE=true` quando a aplicação estiver
atrás de HTTPS.

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

Como a autenticação utiliza cookies, operações mutáveis exigem token CSRF. Para
testes manuais, faça primeiro `GET /api/auth/csrf`, envie o cookie recebido e
repita o valor no header `X-XSRF-TOKEN`.

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
