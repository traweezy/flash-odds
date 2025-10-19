package com.flashodds.backend.provider.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OddsApiBookmaker(
        String key,
        String title,
        List<OddsApiMarket> markets) {
}
