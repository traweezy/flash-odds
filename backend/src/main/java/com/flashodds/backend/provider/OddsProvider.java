package com.flashodds.backend.provider;

import java.util.List;

import com.flashodds.backend.domain.OddsRow;

import reactor.core.publisher.Mono;

public interface OddsProvider {

    Mono<List<OddsRow>> fetchOdds(OddsQuery query);

    String name();
}
