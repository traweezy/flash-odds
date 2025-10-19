package com.flashodds.backend.config;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotBlank String adminUsername,
        @NotBlank String adminPassword) {
}
