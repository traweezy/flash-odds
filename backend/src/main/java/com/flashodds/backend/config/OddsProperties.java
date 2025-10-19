package com.flashodds.backend.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.time.DurationMin;

@ConfigurationProperties(prefix = "app.odds")
public record OddsProperties(
        @NotBlank String provider,
        String apiKey,
        @NotBlank String regions,
        @NotNull @NotEmpty List<String> sports,
        @NotNull @NotEmpty List<String> markets,
        boolean enrichmentEnabled,
        @DurationUnit(ChronoUnit.SECONDS) @DurationMin(seconds = 5) Duration refreshSeconds,
        @DurationUnit(ChronoUnit.SECONDS) @DurationMin(seconds = 1) Duration cacheTtlSeconds,
        DataSize maxPayloadSize) {

    public OddsProperties {
        sports = sports == null ? List.of() : List.copyOf(sports);
        markets = markets == null ? List.of() : List.copyOf(markets);
        refreshSeconds = refreshSeconds == null ? Duration.ofSeconds(15) : refreshSeconds;
        cacheTtlSeconds = cacheTtlSeconds == null ? Duration.ofSeconds(60) : cacheTtlSeconds;
        maxPayloadSize = maxPayloadSize == null ? DataSize.ofMegabytes(2) : maxPayloadSize;
    }
}
