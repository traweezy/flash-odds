package com.flashodds.backend.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.flashodds.backend.domain.OddsRow;
import com.flashodds.backend.web.dto.OddsFrameDto;
import com.flashodds.backend.service.OddsService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/odds")
public class OddsRestController {

    private final OddsService oddsService;
    private final OddsMapper mapper;

    public OddsRestController(OddsService oddsService, OddsMapper mapper) {
        this.oddsService = oddsService;
        this.mapper = mapper;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<OddsRow>> listOdds(
            @RequestParam(name = "sport", required = false) List<String> sports,
            @RequestParam(name = "market", required = false) List<String> markets) {
        return oddsService.currentOdds()
                .map(rows -> rows.stream()
                        .filter(row -> sports == null || sports.isEmpty() || sports.contains(row.sport()))
                        .filter(row -> markets == null || markets.isEmpty() || markets.contains(row.market()))
                        .toList());
    }

    @GetMapping(path = "/frame", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<OddsFrameDto> snapshotFrame() {
        return oddsService.latestSnapshot()
                .switchIfEmpty(oddsService.refreshNow().then(oddsService.latestSnapshot()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Odds not ready")))
                .map(mapper::toDto);
    }
}
