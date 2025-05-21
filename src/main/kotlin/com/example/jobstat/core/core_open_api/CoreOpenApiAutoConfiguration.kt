package com.example.jobstat.core.core_open_api // 이전 패키지명 유지 또는 com.example.jobstat.core.openapi.autoconfigure 로 변경

import com.example.jobstat.core.core_open_api.converter.CustomModelConverter
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.util.StringUtils
import org.springframework.boot.autoconfigure.condition.AllNestedConditions // AND 조건 결합용
import org.springframework.context.annotation.Conditional // 커스텀 조건 결합용
import org.springframework.context.annotation.ConfigurationCondition

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
    // 여러 @ConditionalOnProperty를 결합하기 위해 @Conditional 사용
    @Conditional(CustomModelConverterCondition::class)
    fun customModelConverter(): CustomModelConverter {
        log.info("Registering CustomModelConverter bean.")
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

    @ConditionalOnProperty(
        name = ["spring.profiles.active"],
        matchIfMissing = true, // 프로파일이 없으면 개발 환경으로 간주하여 true
        havingValue = "!prod"  // 'prod' 프로파일이 아니어야 함
    )
    class OnNotProductionProfile
}