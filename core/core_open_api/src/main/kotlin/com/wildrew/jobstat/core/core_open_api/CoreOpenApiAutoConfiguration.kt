package com.wildrew.jobstat.core.core_open_api// 이전 패키지명 유지 또는 com.wildrew.jobstat.core.openapi.autoconfigure 로 변경

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.slf4j.LoggerFactory
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.ConfigurationCondition
import org.springframework.context.annotation.Profile
import org.springframework.util.StringUtils

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI::class, GroupedOpenApi::class)
@ConditionalOnProperty(name = ["springdoc.api-docs.enabled"], havingValue = "true", matchIfMissing = true) // 전체 AutoConfig 활성화 조건
class CoreOpenApiAutoConfiguration(
) {

    private val log = LoggerFactory.getLogger(CoreOpenApiAutoConfiguration::class.java)

    // --- API 정보 프로퍼티 ---
    @Value("\${jobstat.core.openapi.title:Default API Title}")
    private lateinit var title: String

    @Value("\${jobstat.core.openapi.version:v1.0.0}")
    private lateinit var version: String

    @Value("\${jobstat.core.openapi.description:Default API Description}")
    private lateinit var description: String

    // --- Contact 정보 프로퍼티 (선택적) ---
    @Value("\${jobstat.core.openapi.contact.name:}")
    private lateinit var contactName: String

    @Value("\${jobstat.core.openapi.contact.email:}") // 누락된 변수 선언 추가
    private lateinit var contactEmail: String

    @Value("\${jobstat.core.openapi.contact.url:}") // 누락된 변수 선언 추가
    private lateinit var contactUrl: String

    // --- License 정보 프로퍼티 (선택적) ---
    @Value("\${jobstat.core.openapi.license.name:}") // 누락된 변수 선언 추가
    private lateinit var licenseName: String

    @Value("\${jobstat.core.openapi.license.url:}") // 누락된 변수 선언 추가
    private lateinit var licenseUrl: String

    // --- 서버 정보 프로퍼티 ---
    @Value("\${jobstat.core.openapi.server.urls:}")
    private lateinit var serverUrls: String

    @Value("\${jobstat.core.openapi.server.description:Default Server}")
    private lateinit var serverDescription: String

    // --- 보안 설정 프로퍼티 ---
    @Value("\${jobstat.core.openapi.security.bearer-auth.enabled:true}")
    private var bearerAuthEnabled: Boolean = true

    private val jwtSecuritySchemeName = "bearerAuth"


    @Bean
    @ConditionalOnMissingBean(OpenAPI::class)
    fun customOpenApiDefinition(): OpenAPI {
        log.info("Configuring custom OpenAPI definition. Title: '{}'", title)

        val openApiInfo = Info().apply {
            title(this@CoreOpenApiAutoConfiguration.title)
            version(this@CoreOpenApiAutoConfiguration.version)
            description(this@CoreOpenApiAutoConfiguration.description)

            val contact = Contact()
            if (StringUtils.hasText(contactName)) contact.name(contactName)
            if (StringUtils.hasText(contactEmail)) contact.email(contactEmail) // 이제 변수 사용 가능
            if (StringUtils.hasText(contactUrl)) contact.url(contactUrl)       // 이제 변수 사용 가능
            if (contact.name != null || contact.email != null || contact.url != null) {
                this.contact = contact
            }

            val license = License()
            if (StringUtils.hasText(licenseName)) license.name(licenseName) // 이제 변수 사용 가능
            if (StringUtils.hasText(licenseUrl)) license.url(licenseUrl)    // 이제 변수 사용 가능
            if (license.name != null || license.url != null) {
                this.license = license
            }
        }

        val openApi = OpenAPI().info(openApiInfo)

        if (StringUtils.hasText(serverUrls)) {
            val urls = serverUrls.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (urls.isNotEmpty()) {
                openApi.servers(urls.map { Server().url(it).description(serverDescription) })
            }
        } else {
            log.warn("No server URL configured via 'jobstat.core.openapi.server.urls'. Using default behavior.")
        }

        if (bearerAuthEnabled) {
            openApi.components(
                Components().addSecuritySchemes(
                    jwtSecuritySchemeName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .`in`(SecurityScheme.In.HEADER)
                        .name("Authorization")
                )
            )
            openApi.addSecurityItem(SecurityRequirement().addList(jwtSecuritySchemeName))
        }
        return openApi
    }

    /**
     * CustomModelConverter를 빈으로 등록합니다.
     * jobstat.core.openapi.custom-model-converter.enabled=true (기본값 true) 이고,
     * 'prod' 프로파일이 아닐 때만 등록됩니다.
     */
    @Bean
    @ConditionalOnMissingBean(CustomModelConverter::class)
    @ConditionalOnProperty( // 첫 번째 조건
        name = ["jobstat.core.openapi.custom-model-converter.enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    @Profile("!prod")
    fun customModelConverter(): CustomModelConverter {
        log.info("Registering com.wildrew.jobstat.core.core_open_api.CustomModelConverter bean.")
        return CustomModelConverter()
    }
}


class CustomModelConverterCondition : AllNestedConditions(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN) {

    @ConditionalOnProperty(
        name = ["jobstat.core.openapi.custom-model-converter.enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    class OnCustomModelConverterEnabled

    @Profile("!prod")
    class OnNotProductionProfile
}