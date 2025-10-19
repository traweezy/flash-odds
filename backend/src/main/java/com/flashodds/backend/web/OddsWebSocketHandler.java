package com.flashodds.backend.web;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashodds.backend.web.dto.OddsFrameDto;
import com.flashodds.backend.service.OddsService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OddsWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OddsWebSocketHandler.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(20);

    private final OddsService oddsService;
    private final OddsMapper mapper;
    private final ObjectMapper objectMapper;

    public OddsWebSocketHandler(OddsService oddsService, OddsMapper mapper, ObjectMapper objectMapper) {
        this.oddsService = oddsService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var oddsFlux = oddsService.streamFrames()
                .map(mapper::toDto)
                .map(dto -> toTextMessage(session, dto));

        var heartbeatFlux = Flux.interval(HEARTBEAT_INTERVAL)
                .map(tick -> session.textMessage("{\"type\":\"ping\",\"ts\":" + Instant.now().toEpochMilli() + "}"));

        return session.send(Flux.merge(oddsFlux, heartbeatFlux))
                .and(session.receive()
                        .doOnNext(WebSocketMessage::retain)
                        .doOnNext(message -> log.trace("Received websocket message: {}", message.getPayloadAsText()))
                        .then());
    }

    private WebSocketMessage toTextMessage(WebSocketSession session, OddsFrameDto dto) {
        try {
            return session.textMessage(objectMapper.writeValueAsString(dto));
        } catch (IOException ex) {
            log.error("Failed to encode odds frame", ex);
            throw new IllegalStateException("Unable to encode frame");
        }
    }
}
