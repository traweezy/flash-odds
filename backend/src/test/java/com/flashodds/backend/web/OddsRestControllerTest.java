package com.flashodds.backend.web;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.flashodds.backend.domain.OddsFrame;
import com.flashodds.backend.domain.OddsFrameType;
import com.flashodds.backend.domain.OddsRow;
import com.flashodds.backend.domain.OddsRowChange;
import com.flashodds.backend.service.OddsService;

import reactor.core.publisher.Mono;

class OddsRestControllerTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private OddsService oddsService;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        oddsService = Mockito.mock(OddsService.class);
        when(oddsService.refreshNow()).thenReturn(Mono.empty());
        var mapper = new OddsMapper();
        client = WebTestClient.bindToController(new OddsRestController(oddsService, mapper)).build();
    }

    @Test
    void returnsOddsList() {
        when(oddsService.currentOdds()).thenReturn(Mono.just(List.of(sampleRow())));

        client.get()
                .uri("/api/odds")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("nba:test:flashbet:h2h:home");
    }

    @Test
    void returnsSnapshotFrame() {
        var frame = new OddsFrame(OddsFrameType.SNAPSHOT, Instant.now(),
                List.of(new OddsRowChange(OddsRowChange.Operation.UPSERT, sampleRow(), "nba:test:flashbet:h2h:home")));
        when(oddsService.latestSnapshot()).thenReturn(Mono.just(frame));

        client.get()
                .uri("/api/odds/frame")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.type").isEqualTo("snapshot");
    }

    private OddsRow sampleRow() {
        return new OddsRow(
                "nba:test:flashbet:h2h:home",
                "nba",
                "Team A vs Team B",
                "h2h",
                null,
                110,
                "FlashBet",
                Instant.now(),
                Instant.now(),
                Map.of());
    }
}
