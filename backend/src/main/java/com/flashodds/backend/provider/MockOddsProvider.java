package com.flashodds.backend.provider;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import com.flashodds.backend.domain.OddsRow;

import reactor.core.publisher.Mono;

@Component
public class MockOddsProvider implements OddsProvider {

    private static final List<MockEvent> EVENTS = List.of(
            new MockEvent("nfl", "NFL", "sb-59",
                    "Kansas City Chiefs vs San Francisco 49ers",
                    Instant.now().plus(30, ChronoUnit.DAYS)),
            new MockEvent("nba", "NBA", "bos-den",
                    "Boston Celtics vs Denver Nuggets",
                    Instant.now().plus(12, ChronoUnit.HOURS)),
            new MockEvent("nhl", "NHL", "nyr-bos",
                    "New York Rangers vs Boston Bruins",
                    Instant.now().plus(3, ChronoUnit.DAYS)),
            new MockEvent("mlb", "MLB", "nyy-lad",
                    "New York Yankees vs Los Angeles Dodgers",
                    Instant.now().plus(18, ChronoUnit.HOURS)));

    private static final List<String> BOOKS = List.of("FlashBet", "NorthStar", "EdgePlay");

    private final RandomGenerator random = RandomGenerator.of("Xoshiro256PlusPlus");

    @Override
    public Mono<List<OddsRow>> fetchOdds(OddsQuery query) {
        var rows = new ArrayList<OddsRow>();
        var allowedSports = query.sports() == null || query.sports().isEmpty()
                ? EVENTS.stream().map(MockEvent::sport).toList()
                : query.sports();
        var allowedMarkets = query.markets() == null || query.markets().isEmpty()
                ? List.of("h2h", "spreads", "totals")
                : query.markets();

        for (var event : EVENTS) {
            if (!allowedSports.contains(event.sport())) {
                continue;
            }
            for (var book : BOOKS) {
                rows.addAll(buildForEvent(event, book, allowedMarkets));
            }
        }

        return Mono.just(rows);
    }

    @Override
    public String name() {
        return "mock";
    }

    private List<OddsRow> buildForEvent(MockEvent event, String book, List<String> markets) {
        var rows = new ArrayList<OddsRow>();
        var now = Instant.now();
        for (var market : markets) {
            switch (market) {
                case "h2h" -> rows.addAll(headToHeadRows(event, book, now));
                case "spreads" -> rows.addAll(spreadRows(event, book, now));
                case "totals" -> rows.addAll(totalRows(event, book, now));
                default -> {
                }
            }
        }
        return rows;
    }

    private List<OddsRow> headToHeadRows(MockEvent event, String book, Instant now) {
        return List.of(
                buildRow(event, book, "h2h", event.homeTeam(), null, event.homeTeam()),
                buildRow(event, book, "h2h", event.awayTeam(), null, event.awayTeam()));
    }

    private List<OddsRow> spreadRows(MockEvent event, String book, Instant now) {
        return List.of(
                buildRow(event, book, "spreads", event.homeTeam(),
                        roundHalf(random.nextDouble(-7.5, -0.5)),
                        event.homeTeam()),
                buildRow(event, book, "spreads", event.awayTeam(),
                        roundHalf(random.nextDouble(0.5, 7.5)),
                        event.awayTeam()));
    }

    private List<OddsRow> totalRows(MockEvent event, String book, Instant now) {
        var total = roundHalf(random.nextDouble(180.5, 240.5));
        return List.of(
                buildRow(event, book, "totals", "Over", total, "over"),
                buildRow(event, book, "totals", "Under", total, "under"));
    }

    private OddsRow buildRow(MockEvent event, String book, String market, String participant, Double line,
            String runnerKey) {
        return new OddsRow(
                event.sport() + ":" + event.league() + ":" + event.key() + ":" + book.toLowerCase() + ":" + market
                        + ":" + runnerKey.toLowerCase().replace(" ", "-"),
                event.sport(),
                event.display(),
                market,
                line,
                randomAmericanOdds(),
                book,
                event.startsAt(),
                Instant.now(),
                Map.of(
                        "league", event.league(),
                        "participant", participant,
                        "source", "mock"));
    }

    private int randomAmericanOdds() {
        var base = random.nextInt(70, 140);
        return random.nextBoolean() ? base : -base;
    }

    private double roundHalf(double value) {
        return Math.round(value * 2.0) / 2.0;
    }

    private record MockEvent(
            String sport,
            String league,
            String key,
            String display,
            Instant startsAt) {

        String homeTeam() {
            return display.split(" vs ")[0];
        }

        String awayTeam() {
            return display.split(" vs ")[1];
        }
    }
}
