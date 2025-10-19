package com.flashodds.backend.domain;

import java.time.Instant;
import java.util.Map;

public record OddsRow(
        String id,
        String sport,
        String event,
        String market,
        Double line,
        Integer price,
        String book,
        Instant startsAt,
        Instant updatedAt,
        Map<String, Object> extra) {

    public OddsRow {
        extra = extra == null ? Map.of() : Map.copyOf(extra);
    }
}
