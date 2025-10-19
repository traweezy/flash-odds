package com.flashodds.backend.provider;

import java.util.List;

public record OddsQuery(
        List<String> sports,
        List<String> markets,
        String regions) {
}
