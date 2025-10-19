package com.flashodds.backend.web.error;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        var detail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : "");
        detail.setTitle("Request failed");
        detail.setType(URI.create("https://flashodds.app/problems/" + ex.getStatusCode().value()));
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        var detail = ProblemDetail.forStatus(500);
        detail.setTitle("Internal Server Error");
        detail.setDetail("We could not complete the request.");
        detail.setType(URI.create("https://flashodds.app/problems/internal-server-error"));
        return detail;
    }
}
