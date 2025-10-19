package com.flashodds.backend.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.flashodds.backend.domain.OddsRow;
import com.flashodds.backend.domain.OddsRowChange;

public final class OddsDiffCalculator {

    private OddsDiffCalculator() {
    }

    public static List<OddsRowChange> diff(Map<String, OddsRow> previous, Map<String, OddsRow> fresh) {
        var changes = new ArrayList<OddsRowChange>();
        for (var entry : fresh.entrySet()) {
            var prior = previous.get(entry.getKey());
            if (prior == null || !prior.equals(entry.getValue())) {
                changes.add(new OddsRowChange(OddsRowChange.Operation.UPSERT, entry.getValue(), entry.getKey()));
            }
        }
        for (var entry : previous.entrySet()) {
            if (!fresh.containsKey(entry.getKey())) {
                changes.add(new OddsRowChange(OddsRowChange.Operation.REMOVE, null, entry.getKey()));
            }
        }
        return changes;
    }
}
