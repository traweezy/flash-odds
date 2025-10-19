package com.flashodds.backend.web.dto;

import com.flashodds.backend.domain.OddsRow;

public record OddsRowChangeDto(
        String op,
        String id,
        OddsRow row) {
}
