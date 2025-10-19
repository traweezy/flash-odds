package com.flashodds.backend.provider.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OddsApiEvent(
        String id,
        @JsonProperty("sport_key") String sportKey,
        @JsonProperty("sport_title") String sportTitle,
        @JsonProperty("commence_time") String commenceTime,
        @JsonProperty("home_team") String homeTeam,
        @JsonProperty("away_team") String awayTeam,
        List<OddsApiBookmaker> bookmakers) {

    public Instant commenceInstant() {
        if (commenceTime == null || commenceTime.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(commenceTime);
        } catch (Exception ignored) {
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(commenceTime));
        } catch (Exception ignored) {
        }
        try {
            return Instant.ofEpochSecond((long) Double.parseDouble(commenceTime));
        } catch (Exception ignored) {
        }
        return Instant.now();
    }

    public String displayTitle() {
        if (homeTeam != null && awayTeam != null && !homeTeam.isBlank() && !awayTeam.isBlank()) {
            return homeTeam + " vs " + awayTeam;
        }
        if (sportTitle != null && !sportTitle.isBlank()) {
            return sportTitle;
        }
        return sportKey != null ? sportKey : "event";
    }
}
