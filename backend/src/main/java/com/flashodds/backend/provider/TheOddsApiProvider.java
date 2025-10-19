package com.flashodds.backend.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.flashodds.backend.config.OddsProperties;
import com.flashodds.backend.domain.OddsRow;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TheOddsApiProvider implements OddsProvider {

    private static final Logger log = LoggerFactory.getLogger(TheOddsApiProvider.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final OddsProperties properties;

    public TheOddsApiProvider(WebClient oddsWebClient, OddsProperties properties) {
        this.webClient = oddsWebClient.mutate()
                .baseUrl("https://api.the-odds-api.com/v4")
                .build();
        this.properties = properties;
    }

    @Override
    public Mono<List<OddsRow>> fetchOdds(OddsQuery query) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return Mono.error(new IllegalStateException("ODDS_API_KEY is required for theoddsapi provider"));
        }

        return Flux.fromIterable(query.sports())
                .flatMap(sport -> fetchSport(sport, query))
                .collectList()
                .map(list -> list.stream().flatMap(List::stream).toList());
    }

    private Mono<List<OddsRow>> fetchSport(String sport, OddsQuery query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/sports/{sport}/odds")
                        .queryParam("apiKey", properties.apiKey())
                        .queryParam("regions", query.regions())
                        .queryParam("markets", String.join(",", query.markets()))
                        .queryParam("oddsFormat", "american")
                        .queryParam("dateFormat", "unix")
                        .build(sport))
                .retrieve()
                .bodyToMono(RESPONSE_TYPE)
                .map(events -> mapEvents(sport, events))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        log.warn("The Odds API rate limit reached: {}", ex.getMessage());
                    } else {
                        log.warn("The Odds API error [{}]: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    }
                    return Mono.just(List.of());
                })
                .onErrorResume(ex -> {
                    log.warn("Failed to fetch odds for sport {}: {}", sport, ex.getMessage());
                    return Mono.just(List.of());
                });
    }

    private List<OddsRow> mapEvents(String sport, List<Map<String, Object>> events) {
        var rows = new ArrayList<OddsRow>();
        for (var event : events) {
            var id = stringValue(event.get("id"));
            var commence = parseInstant(event.get("commence_time"));
            var homeTeam = stringValue(event.getOrDefault("home_team", ""));
            var awayTeam = stringValue(event.getOrDefault("away_team", ""));
            var title = homeTeam.isBlank() || awayTeam.isBlank()
                    ? stringValue(event.getOrDefault("sport_title", ""))
                    : homeTeam + " vs " + awayTeam;
            var bookmakers = (List<Map<String, Object>>) event.getOrDefault("bookmakers", List.of());
            for (var book : bookmakers) {
                var bookKey = stringValue(book.get("key"));
                var bookTitle = stringValue(book.getOrDefault("title", bookKey));
                var markets = (List<Map<String, Object>>) book.getOrDefault("markets", List.of());
                rows.addAll(mapMarkets(sport, id, title, commence, bookKey, bookTitle, markets));
            }
        }
        return rows;
    }

    private List<OddsRow> mapMarkets(
            String sport,
            String eventKey,
            String title,
            Instant commence,
            String bookKey,
            String bookTitle,
            List<Map<String, Object>> markets) {
        var rows = new ArrayList<OddsRow>();
        for (var market : markets) {
            var marketKey = stringValue(market.get("key"));
            var outcomes = (List<Map<String, Object>>) market.getOrDefault("outcomes", List.of());
            for (var outcome : outcomes) {
                var outcomeName = stringValue(outcome.get("name"));
                var price = toAmericanOdds(outcome.get("price"));
                var point = toDouble(outcome.get("point"));
                var rowId = String.join(":",
                        List.of(
                                sport,
                                eventKey,
                                bookKey,
                                marketKey,
                                normalize(outcomeName)));
                rows.add(new OddsRow(
                        rowId,
                        sport,
                        title,
                        marketKey,
                        point,
                        price,
                        bookTitle,
                        commence,
                        Instant.now(),
                        Map.of("source", "theoddsapi")));
            }
        }
        return rows;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private Instant parseInstant(Object value) {
        if (value instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Instant.parse(text);
            } catch (Exception ignored) {
            }
        }
        return Instant.now();
    }

    private Integer toAmericanOdds(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return (int) Math.round(number.doubleValue());
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace(" ", "-");
    }

    @Override
    public String name() {
        return "theoddsapi";
    }
}
