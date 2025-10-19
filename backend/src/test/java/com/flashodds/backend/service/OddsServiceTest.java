package com.flashodds.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import com.flashodds.backend.config.OddsProperties;
import com.flashodds.backend.domain.OddsFrameType;
import com.flashodds.backend.domain.OddsRow;
import com.flashodds.backend.provider.OddsProvider;
import com.flashodds.backend.provider.OddsProviderRegistry;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

class OddsServiceTest {

    private TestProvider provider;
    private OddsService service;

    @BeforeEach
    void setUp() {
        provider = new TestProvider();
        var props = new OddsProperties(
                "mock",
                "",
                "us",
                List.of("nba"),
                List.of("h2h"),
                false,
                Duration.ofSeconds(5),
                Duration.ofSeconds(60),
                DataSize.ofMegabytes(2));
        var registry = new OddsProviderRegistry(List.of(provider));
        service = new OddsService(registry, props, new SimpleMeterRegistry());
    }

    @Test
    void updatesStateAndSnapshot() {
        provider.setRows(List.of(row("nba:event1:mock:h2h:home", "Boston Celtics vs Denver Nuggets", "h2h", 110)));

        service.refreshNow().block();

        service.currentOdds()
                .doOnNext(rows -> assertThat(rows).hasSize(1))
                .block();

        provider.setRows(List.of(
                row("nba:event1:mock:h2h:home", "Boston Celtics vs Denver Nuggets", "h2h", 115),
                row("nba:event1:mock:h2h:away", "Boston Celtics vs Denver Nuggets", "h2h", -120)));

        service.refreshNow().block();

        service.currentOdds()
                .doOnNext(rows -> assertThat(rows).hasSize(2))
                .block();

        service.latestSnapshot()
                .doOnNext(snapshot -> {
                    assertThat(snapshot.type()).isEqualTo(OddsFrameType.SNAPSHOT);
                    assertThat(snapshot.rows()).hasSize(2);
                })
                .block();
    }

    private OddsRow row(String id, String event, String market, int price) {
        return new OddsRow(
                id,
                "nba",
                event,
                market,
                null,
                price,
                "FlashBet",
                Instant.now(),
                Instant.now(),
                Map.of());
    }

    private static class TestProvider implements OddsProvider {

        private final AtomicReference<List<OddsRow>> rows = new AtomicReference<>(List.of());

        @Override
        public Mono<List<OddsRow>> fetchOdds(com.flashodds.backend.provider.OddsQuery query) {
            return Mono.just(rows.get());
        }

        void setRows(List<OddsRow> nextRows) {
            rows.set(nextRows);
        }

        @Override
        public String name() {
            return "mock";
        }
    }
}
