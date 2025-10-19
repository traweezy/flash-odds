package com.flashodds.backend.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OddsApiOutcome(
        String name,
        Double price,
        Double point) {
}
