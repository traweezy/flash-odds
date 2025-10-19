package com.flashodds.backend.web;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.flashodds.backend.domain.OddsFrame;
import com.flashodds.backend.domain.OddsRow;
import com.flashodds.backend.domain.OddsRowChange;
import com.flashodds.backend.web.dto.OddsFrameDto;
import com.flashodds.backend.web.dto.OddsRowChangeDto;

@Component
public class OddsMapper {

    public List<OddsRow> toRows(List<OddsRow> rows) {
        return rows;
    }

    public OddsFrameDto toDto(OddsFrame frame) {
        return new OddsFrameDto(
                frame.type().name().toLowerCase(),
                frame.timestamp() != null ? frame.timestamp() : Instant.now(),
                frame.rows().stream()
                        .map(this::toDto)
                        .toList());
    }

    public OddsRowChangeDto toDto(OddsRowChange change) {
        return new OddsRowChangeDto(
                change.op().name().toLowerCase(),
                change.id(),
                change.op().isUpsert() ? change.row() : null);
    }
}
