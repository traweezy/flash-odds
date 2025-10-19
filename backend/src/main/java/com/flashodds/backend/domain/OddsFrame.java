package com.flashodds.backend.domain;

import java.time.Instant;
import java.util.List;

public record OddsFrame(
        OddsFrameType type,
        Instant timestamp,
        List<OddsRowChange> rows) {

    public OddsFrame {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
