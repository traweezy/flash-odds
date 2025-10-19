package com.flashodds.backend.config;

import java.time.Duration;
import java.util.List;

import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app.odds")
public record OddsProperties(
        @NotBlank String provider,
        String apiKey,
        @NotBlank String regions,
        @NotNull List<String> sports,
        @NotNull List<String> markets,
        boolean enrichmentEnabled,
        @DurationUnit(ChronoUnit.SECONDS) Duration refreshSeconds,
        @DurationUnit(ChronoUnit.SECONDS) Duration cacheTtlSeconds,
        DataSize maxPayloadSize) {

    public OddsProperties {
        sports = sports == null ? List.of() : List.copyOf(sports);
        markets = markets == null ? List.of() : List.copyOf(markets);
        refreshSeconds = refreshSeconds == null ? Duration.ofSeconds(15) : refreshSeconds;
        cacheTtlSeconds = cacheTtlSeconds == null ? Duration.ofSeconds(60) : cacheTtlSeconds;
        maxPayloadSize = maxPayloadSize == null ? DataSize.ofMegabytes(2) : maxPayloadSize;
    }
}
