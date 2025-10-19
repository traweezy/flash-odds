package com.flashodds.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flashOddsOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("FlashOdds 25 API")
                        .description("Live odds feed with REST/WS/SSE endpoints")
                        .version("v0.1.0"));
    }
}
