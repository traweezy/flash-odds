package com.flashodds.backend.web.dto;

import java.time.Instant;
import java.util.List;

public record OddsFrameDto(
        String type,
        Instant ts,
        List<OddsRowChangeDto> rows) {
}
