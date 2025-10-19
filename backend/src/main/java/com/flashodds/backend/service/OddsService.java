package com.flashodds.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.flashodds.backend.config.OddsProperties;
import com.flashodds.backend.domain.OddsFrame;
import com.flashodds.backend.domain.OddsFrameType;
import com.flashodds.backend.domain.OddsRow;
import com.flashodds.backend.domain.OddsRowChange;
import com.flashodds.backend.provider.OddsProvider;
import com.flashodds.backend.provider.OddsProviderRegistry;
import com.flashodds.backend.provider.OddsQuery;
import com.flashodds.backend.support.OddsDiffCalculator;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
public class OddsService {

    private static final Logger log = LoggerFactory.getLogger(OddsService.class);

    private final OddsProviderRegistry providerRegistry;
    private final OddsProperties properties;

    private final ReentrantLock refreshLock = new ReentrantLock();
    private final AtomicReference<Map<String, OddsRow>> currentState = new AtomicReference<>(Map.of());
    private final AtomicReference<OddsFrame> lastSnapshot = new AtomicReference<>();

    private final Sinks.Many<OddsFrame> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer(512, false);

    private final Timer refreshTimer;
    private final Counter refreshErrors;

    private Disposable refreshLoop;

    public OddsService(OddsProviderRegistry providerRegistry, OddsProperties properties, MeterRegistry meterRegistry) {
        this.providerRegistry = providerRegistry;
        this.properties = properties;
        this.refreshTimer = meterRegistry.timer("flashodds.odds.refresh");
        this.refreshErrors = meterRegistry.counter("flashodds.odds.refresh.errors");
    }

    @PostConstruct
    void start() {
        refreshLoop = Flux.interval(Duration.ZERO, properties.refreshSeconds())
                .flatMap(tick -> refreshOnce()
                        .doOnError(ex -> log.warn("Odds refresh failed: {}", ex.getMessage(), ex))
                        .onErrorResume(ex -> Mono.empty()))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    @PreDestroy
    void shutdown() {
        if (refreshLoop != null && !refreshLoop.isDisposed()) {
            refreshLoop.dispose();
        }
    }

    public Flux<OddsFrame> streamFrames() {
        return Flux.defer(() -> {
            var snapshot = lastSnapshot.get();
            var initial = snapshot != null ? Mono.just(snapshot) : Mono.<OddsFrame>empty();
            return initial.concatWith(sink.asFlux());
        });
    }

    public Mono<List<OddsRow>> currentOdds() {
        return Mono.fromSupplier(() -> currentState.get().values().stream()
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList());
    }

    public Mono<OddsFrame> latestSnapshot() {
        return Mono.justOrEmpty(lastSnapshot.get());
    }

    public Mono<Void> refreshNow() {
        return refreshOnce();
    }

    private Mono<Void> refreshOnce() {
        var provider = resolveProvider();
        var sample = Timer.start();
        return provider.fetchOdds(new OddsQuery(properties.sports(), properties.markets(), properties.regions()))
                .map(this::indexRows)
                .flatMap(this::applyDiff)
                .doOnError(ex -> {
                    refreshErrors.increment();
                    sample.stop(refreshTimer);
                })
                .doOnSuccess(ignored -> sample.stop(refreshTimer))
                .then();
    }

    private OddsProvider resolveProvider() {
        try {
            return providerRegistry.get(properties.provider());
        } catch (IllegalArgumentException ex) {
            log.warn("Requested odds provider '{}' not found, falling back to mock", properties.provider());
            return providerRegistry.get("mock");
        }
    }

    private Map<String, OddsRow> indexRows(List<OddsRow> rows) {
        return rows.stream().collect(Collectors.toMap(OddsRow::id, row -> row, (left, right) -> right));
    }

    private Mono<Void> applyDiff(Map<String, OddsRow> fresh) {
        return Mono.fromRunnable(() -> {
            refreshLock.lock();
            try {
                var previous = currentState.get();
                var snapshot = buildSnapshot(fresh);
                lastSnapshot.set(snapshot);
                currentState.set(fresh);

                if (previous.isEmpty()) {
                    emit(snapshot);
                    return;
                }

                var changes = OddsDiffCalculator.diff(previous, fresh);
                if (changes.isEmpty()) {
                    return;
                }
                var frame = new OddsFrame(OddsFrameType.DELTA, Instant.now(), changes);
                emit(frame);
            } finally {
                refreshLock.unlock();
            }
        });
    }

    private OddsFrame buildSnapshot(Map<String, OddsRow> rows) {
        var changes = rows.values().stream()
                .map(row -> new OddsRowChange(OddsRowChange.Operation.UPSERT, row, row.id()))
                .toList();
        return new OddsFrame(OddsFrameType.SNAPSHOT, Instant.now(), changes);
    }

    private void emit(OddsFrame frame) {
        var result = sink.tryEmitNext(frame);
        if (result.isFailure()) {
            log.debug("Dropping odds frame due to backpressure: {}", result);
        }
    }
}
