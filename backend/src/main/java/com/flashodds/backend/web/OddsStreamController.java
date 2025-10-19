package com.flashodds.backend.web;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flashodds.backend.web.dto.OddsFrameDto;
import com.flashodds.backend.service.OddsService;

import java.time.Duration;

import reactor.core.publisher.Flux;

@RestController
public class OddsStreamController {

    private final OddsService oddsService;
    private final OddsMapper mapper;

    public OddsStreamController(OddsService oddsService, OddsMapper mapper) {
        this.oddsService = oddsService;
        this.mapper = mapper;
    }

    @GetMapping(path = "/api/odds/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OddsFrameDto>> stream() {
        var frames = oddsService.streamFrames()
                .map(mapper::toDto)
                .map(frame -> ServerSentEvent.<OddsFrameDto>builder(frame)
                        .event(frame.type())
                        .build());

        var heartbeat = Flux.interval(Duration.ofSeconds(20))
                .map(tick -> ServerSentEvent.<OddsFrameDto>builder()
                        .event("ping")
                        .comment("heartbeat")
                        .build());

        return Flux.merge(frames, heartbeat);
    }
}
