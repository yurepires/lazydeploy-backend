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
- `POST /api/bf4/subscriptions`
- `GET /api/bf4/subscriptions`
- `GET /api/bf4/subscriptions/{id}`
- `DELETE /api/bf4/subscriptions/{id}`
- `GET|POST /api/bf4/subscriptions/{id}/rules`
- `PUT|DELETE /api/bf4/subscriptions/{id}/rules/{ruleId}`

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

Records são utilizados apenas em tipos de dados imutáveis, como valores do domínio,
configurações e respostas de integrações. Controllers, services, entidades JPA e DTOs
da API são classes separadas e ficam em arquivos próprios.
