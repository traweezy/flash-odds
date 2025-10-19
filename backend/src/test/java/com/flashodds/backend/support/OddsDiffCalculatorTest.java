package com.flashodds.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.flashodds.backend.domain.OddsRow;
import com.flashodds.backend.domain.OddsRowChange;

class OddsDiffCalculatorTest {

    @Test
    void detectsUpsertsAndRemovals() {
        var previous = Map.of(
                "key-a", row("key-a", 100),
                "key-b", row("key-b", -110));

        var fresh = Map.of(
                "key-a", row("key-a", 105),
                "key-c", row("key-c", 120));

        var changes = OddsDiffCalculator.diff(previous, fresh);

        assertThat(changes)
                .hasSize(3)
                .extracting(OddsRowChange::op, OddsRowChange::id)
                .containsExactlyInAnyOrder(
                        tuple(OddsRowChange.Operation.UPSERT, "key-a"),
                        tuple(OddsRowChange.Operation.UPSERT, "key-c"),
                        tuple(OddsRowChange.Operation.REMOVE, "key-b"));
    }

    private OddsRow row(String id, int price) {
        return new OddsRow(
                id,
                "nba",
                "Game",
                "h2h",
                null,
                price,
                "FlashBet",
                Instant.now(),
                Instant.now(),
                Map.of());
    }
}
