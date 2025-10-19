package com.flashodds.backend.web;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import reactor.core.publisher.Mono;

@Controller
public class SpaForwardingController {

    private final Resource indexHtml;

    public SpaForwardingController(ResourceLoader resourceLoader) {
        this.indexHtml = resourceLoader.getResource("classpath:/static/index.html");
    }

    @RequestMapping(value = { "/", "/{path:^(?!api|ws|actuator).*$}/**" }, produces = MediaType.TEXT_HTML_VALUE)
    public Mono<Resource> forward() {
        return Mono.just(indexHtml);
    }
}
