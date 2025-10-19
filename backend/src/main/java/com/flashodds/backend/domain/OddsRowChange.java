package com.flashodds.backend.domain;

import java.util.Objects;

public record OddsRowChange(
        OddsRowChange.Operation op,
        OddsRow row,
        String id) {

    public OddsRowChange {
        if (op == Operation.UPSERT && row == null) {
            throw new IllegalArgumentException("UPSERT changes require a row payload");
        }
        if (op == Operation.REMOVE && (id == null || id.isBlank())) {
            throw new IllegalArgumentException("REMOVE changes require an id");
        }
        if (op == Operation.UPSERT && (id == null || id.isBlank())) {
            id = row.id();
        }
    }

    public enum Operation {
        UPSERT,
        REMOVE;

        public boolean isUpsert() {
            return this == UPSERT;
        }
    }
}
