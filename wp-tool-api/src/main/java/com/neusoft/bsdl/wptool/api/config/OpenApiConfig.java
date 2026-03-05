package com.neusoft.bsdl.wptool.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wpToolOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WP Tool API")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi wpToolApiGroup() {
        return GroupedOpenApi.builder()
                .group("wp-tool")
                .pathsToMatch("/api/**")
                .build();
    }
}
