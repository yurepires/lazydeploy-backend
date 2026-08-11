package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerPageResponse;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;

@Component
public class BfListClient {

    private static final Logger log = LoggerFactory.getLogger(BfListClient.class);
    private static final int MAX_PAGES_PER_SEARCH = 100;
    private static final int MAX_CURSOR_RESTARTS = 1;

    private final WebClient webClient;
    private final Bf4Properties properties;

    public BfListClient(WebClient webClient, Bf4Properties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public Bf4ServerPageResponse getFirstServerPage() {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/servers")
                        .queryParam("perPage", properties.api().pageSize())
                        .build())
                .retrieve()
                .bodyToMono(Bf4ServerPageResponse.class)
                .block();
    }

    public Bf4ServerPageResponse getNextServerPage(String cursor, String after) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/servers")
                        .queryParam("perPage", properties.api().pageSize())
                        .queryParam("cursor", cursor)
                        .queryParam("after", after)
                        .build())
                .retrieve()
                .bodyToMono(Bf4ServerPageResponse.class)
                .block();
    }

    public Optional<Bf4ServerResponse> findServer(String guid) {
        int cursorRestarts = 0;

        while (true) {
            try {
                return findServerInCurrentSnapshot(guid);
            } catch (WebClientResponseException exception) {
                if (exception.getStatusCode() == HttpStatus.GONE
                        && cursorRestarts < MAX_CURSOR_RESTARTS) {
                    cursorRestarts++;
                    log.warn("Cursor de paginação expirado. Reiniciando a busca pela primeira página.");
                    continue;
                }

                logHttpError(exception);
                return Optional.empty();
            } catch (WebClientRequestException exception) {
                log.error("Falha de rede ao consultar o BFLIST: {}", exception.getMessage());
                return Optional.empty();
            } catch (RuntimeException exception) {
                log.error("Erro inesperado ao percorrer a paginação do BFLIST.", exception);
                return Optional.empty();
            }
        }
    }

    private Optional<Bf4ServerResponse> findServerInCurrentSnapshot(String guid) {
        Bf4ServerPageResponse page = null;

        for (int pageNumber = 1; pageNumber <= MAX_PAGES_PER_SEARCH; pageNumber++) {
            log.info("Consultando página {}...", pageNumber);

            if (pageNumber == 1) {
                page = getFirstServerPage();
            }

            if (page == null) {
                log.warn("BFLIST retornou uma página vazia.");
                return Optional.empty();
            }

            Optional<Bf4ServerResponse> match = findMatch(page.servers(), guid);
            if (match.isPresent()) {
                return match;
            }

            if (!page.hasMore()) {
                return Optional.empty();
            }

            List<Bf4ServerResponse> servers = page.servers();
            if (servers.isEmpty() || page.cursor() == null || page.cursor().isBlank()) {
                log.warn("Paginação inconsistente: hasMore=true sem servidores ou cursor.");
                return Optional.empty();
            }

            Bf4ServerResponse lastServer = servers.getLast();
            String after = lastServer.ip() + ":" + lastServer.port();
            page = getNextServerPage(page.cursor(), after);
        }

        log.warn("Busca interrompida após atingir o limite de {} páginas.", MAX_PAGES_PER_SEARCH);
        return Optional.empty();
    }

    private Optional<Bf4ServerResponse> findMatch(List<Bf4ServerResponse> servers, String guid) {
        return servers.stream()
                .filter(server -> guid != null && guid.equals(server.guid()))
                .findFirst();
    }

    private void logHttpError(WebClientResponseException exception) {
        int status = exception.getStatusCode().value();

        if (status == 400) {
            log.error("BFLIST rejeitou a requisição de paginação (HTTP 400).");
        } else if (status == 404) {
            log.warn("Recurso de listagem do BFLIST não encontrado (HTTP 404).");
        } else if (status == 410) {
            log.warn("Cursor de paginação expirou novamente (HTTP 410); busca atual abortada.");
        } else if (status == 429) {
            log.warn("Limite de requisições do BFLIST atingido (HTTP 429); ciclo atual abortado.");
        } else if (exception.getStatusCode().is5xxServerError()) {
            log.error("BFLIST temporariamente indisponível (HTTP {}).", status);
        } else {
            log.error("Erro HTTP {} ao consultar o BFLIST.", status);
        }
    }
}
