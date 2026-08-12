package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.dto.Bf4ServerPageResponse;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BfListSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(BfListSnapshotService.class);
    private static final int MAX_PAGES_PER_SNAPSHOT = 100;
    private static final int MAX_CURSOR_RESTARTS = 1;

    private final BfListClient client;

    public BfListSnapshotService(BfListClient client) {
        this.client = client;
    }

    public Optional<Bf4ServerSnapshot> fetchSnapshot() {
        int cursorRestarts = 0;

        while (true) {
            try {
                return Optional.of(fetchCurrentSnapshot());
            } catch (WebClientResponseException exception) {
                if (exception.getStatusCode() == HttpStatus.GONE
                        && cursorRestarts < MAX_CURSOR_RESTARTS) {
                    cursorRestarts++;
                    log.warn("Cursor expirado. Reiniciando o snapshot pela primeira página.");
                    continue;
                }

                logHttpError(exception);
                return Optional.empty();
            } catch (WebClientRequestException exception) {
                log.error("Falha de rede ao obter snapshot do BFLIST: {}", exception.getMessage());
                return Optional.empty();
            } catch (RuntimeException exception) {
                log.error("Erro inesperado ao obter snapshot do BFLIST.", exception);
                return Optional.empty();
            }
        }
    }

    private Bf4ServerSnapshot fetchCurrentSnapshot() {
        List<Bf4ServerResponse> allServers = new ArrayList<>();
        Bf4ServerPageResponse page = null;

        for (int pageNumber = 1; pageNumber <= MAX_PAGES_PER_SNAPSHOT; pageNumber++) {
            log.info("Consultando página {} do snapshot BFLIST...", pageNumber);

            if (pageNumber == 1) {
                page = client.getFirstServerPage();
            }

            if (page == null) {
                throw new IllegalStateException("BFLIST retornou uma página sem corpo");
            }

            allServers.addAll(page.servers());

            if (!page.hasMore()) {
                return Bf4ServerSnapshot.from(allServers, Instant.now());
            }

            if (page.servers().isEmpty() || page.cursor() == null || page.cursor().isBlank()) {
                throw new IllegalStateException("hasMore=true sem servidores ou cursor");
            }

            Bf4ServerResponse lastServer = page.servers().getLast();
            String after = lastServer.ip() + ":" + lastServer.port();
            page = client.getNextServerPage(page.cursor(), after);
        }

        throw new IllegalStateException(
                "Snapshot excedeu o limite de " + MAX_PAGES_PER_SNAPSHOT + " páginas"
        );
    }

    private void logHttpError(WebClientResponseException exception) {
        int status = exception.getStatusCode().value();

        if (status == 400) {
            log.error("BFLIST rejeitou a paginação do snapshot (HTTP 400).");
        } else if (status == 404) {
            log.warn("Listagem do BFLIST não encontrada (HTTP 404).");
        } else if (status == 410) {
            log.warn("Cursor expirou novamente (HTTP 410); ciclo abortado.");
        } else if (status == 429) {
            log.warn("Rate limit do BFLIST atingido (HTTP 429); ciclo abortado.");
        } else if (exception.getStatusCode().is5xxServerError()) {
            log.error("BFLIST temporariamente indisponível (HTTP {}).", status);
        } else {
            log.error("Erro HTTP {} ao obter snapshot do BFLIST.", status);
        }
    }
}
