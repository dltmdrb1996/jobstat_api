package com.example.jobstat.core.config

import com.example.jobstat.core.base.mongo.converter.CustomModelConverter
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!prod")
class OpenApiConfig(
    @Value("\${app.server.url}") private val serverUrl: String,
) {
    @Bean
    fun customOpenAPI(): OpenAPI {
        val securityScheme =
            SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .`in`(SecurityScheme.In.HEADER)
                .name("Authorization")

        val securityRequirement = SecurityRequirement().addList("bearerAuth")

        return OpenAPI()
            .info(
                Info()
                    .title("JobStat API")
                    .version("v2")
                    .description("JobStat 애플리케이션의 API 문서"),
            ).servers(
                listOf(
                    Server().url(serverUrl).description("환경에 따른 서버 URL"),
                ),
            ).components(Components().addSecuritySchemes("bearerAuth", securityScheme))
            .addSecurityItem(securityRequirement)
    }

    @PostConstruct
    fun registerCustomModelConverter() {
        ModelConverters.getInstance().addConverter(CustomModelConverter())
    }
}
