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
import com.flashodds.backend.provider.dto.OddsApiBookmaker;
import com.flashodds.backend.provider.dto.OddsApiEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TheOddsApiProvider implements OddsProvider {

    private static final Logger log = LoggerFactory.getLogger(TheOddsApiProvider.class);
    private static final ParameterizedTypeReference<List<OddsApiEvent>> RESPONSE_TYPE =
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

    private List<OddsRow> mapEvents(String sport, List<OddsApiEvent> events) {
        var rows = new ArrayList<OddsRow>();
        for (var event : events) {
            var id = event.id();
            var commence = event.commenceInstant();
            var title = event.displayTitle();
            var bookmakers = event.bookmakers();
            if (bookmakers == null) {
                continue;
            }
            for (var book : bookmakers) {
                rows.addAll(mapMarkets(sport, id, title, commence, book));
            }
        }
        return rows;
    }

    private List<OddsRow> mapMarkets(
            String sport,
            String eventKey,
            String title,
            Instant commence,
            OddsApiBookmaker bookmaker) {
        var rows = new ArrayList<OddsRow>();
        if (bookmaker.markets() == null) {
            return rows;
        }
        for (var market : bookmaker.markets()) {
            var marketKey = market.key();
            var outcomes = market.outcomes();
            if (outcomes == null) {
                continue;
            }
            for (var outcome : outcomes) {
                var outcomeName = outcome.name();
                var price = toAmericanOdds(outcome.price());
                var point = outcome.point();
                var rowId = String.join(":",
                        List.of(
                                sport,
                                eventKey,
                                bookmaker.key(),
                                marketKey,
                                normalize(outcomeName)));
                rows.add(new OddsRow(
                        rowId,
                        sport,
                        title,
                        marketKey,
                        point,
                        price,
                        bookmaker.title() == null || bookmaker.title().isBlank() ? bookmaker.key() : bookmaker.title(),
                        commence,
                        Instant.now(),
                        Map.of("source", "theoddsapi")));
            }
        }
        return rows;
    }

    private Integer toAmericanOdds(Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        return (int) Math.round(value);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace(" ", "-");
    }

    @Override
    public String name() {
        return "theoddsapi";
    }
}
