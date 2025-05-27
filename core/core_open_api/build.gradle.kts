// core-open-api/build.gradle.kts
plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":core:core_web_util")) // CustomModelConverter가 @CommonApiResponseWrapper 참조

    api("io.swagger.core.v3:swagger-models-jakarta") // CustomModelConverter가 ModelConverter, Schema 등 사용
    api("org.springdoc:springdoc-openapi-starter-common:2.8.4") // OpenAPI 빈, GroupedOpenApi 등 제공. UI는 app에서.

    implementation("org.slf4j:slf4j-api")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-beans") // @Value, @Bean 등
    compileOnly("org.springframework:spring-context") // @Configuration
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
