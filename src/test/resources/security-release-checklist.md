# LazyDeploy — checklist de segurança para release

Este checklist deve ser executado em ambiente local ou isolado. Nenhum teste de
carga, brute force ou falha simulada deve apontar para Railway, Cloudflare,
GameTools ou Battlelog Keeper reais.

## Automatizado

- [ ] A suíte Maven completa passa sem falhas.
- [ ] `SecuritySmokeTest` passa.
- [ ] Recursos de outro usuário retornam `404` e não sofrem alteração.
- [ ] Mutations sem CSRF retornam `403` com erro genérico.
- [ ] Origins não confiáveis não são refletidas pelo CORS.
- [ ] Rate limit retorna `429` e `Retry-After`.
- [ ] Payload acima do limite retorna `413` antes do controller.
- [ ] Paginação rejeita valores inválidos e tamanhos acima do máximo.
- [ ] Actuator sensível não está exposto.
- [ ] Providers possuem timeout, retry limitado e bulkhead.
- [ ] Falhas de provider não persistem estado parcial.
- [ ] Logs e métricas não contêm senha, hash, sessão, CSRF, Authorization,
  SMTP, banco, e-mail bruto, IP, GUID de servidor ou subscription ID como tag.

## Validação manual local

- [ ] Login gira o identificador de sessão.
- [ ] Logout invalida a sessão anterior.
- [ ] `JSESSIONID` é `HttpOnly` e, em produção, `Secure` e `SameSite=None`.
- [ ] `XSRF-TOKEN` é legível pelo SPA, mas nunca contém o identificador da sessão.
- [ ] Origin confiável funciona com credentials.
- [ ] Origin não confiável é bloqueada sem `Access-Control-Allow-Origin`.
- [ ] Health, liveness e readiness respondem sem expor detalhes internos.

## Load test controlado

Executar apenas com providers mockados, banco isolado e concorrência pequena:

1. Exercitar leituras de subscriptions e histórico.
2. Enviar tentativas inválidas de login até o `429`.
3. Exercitar busca de servidores até o limite de usuário/IP.
4. Preencher executor, fila e bulkhead gradualmente.
5. Liberar a carga e confirmar recuperação sem restart.

Durante o teste, observar CPU, memória, latência, pool Hikari, executor de
monitoramento, fila e contadores de rejeição. O teste é aprovado somente se a
memória estabilizar, as filas permanecerem limitadas e a aplicação continuar
respondendo após a saturação temporária.

## Bloqueadores de release

Não liberar se houver acesso cross-user, bypass de CSRF, CORS wildcard com
credentials, cookie de sessão inseguro em produção, segredo em logs, Actuator
sensível exposto, limiter contornável, cache/fila sem limite, provider sem
timeout ou sessão reutilizável após logout.
