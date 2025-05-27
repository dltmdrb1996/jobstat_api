import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("io.sentry.jvm.gradle")
}

group = "com.wildrew"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core:core_event"))
    implementation(project(":core:core_error"))
    implementation(project(":core:core_global"))
    implementation(project(":core:core_web_util"))
    implementation(project(":core:core_serializer"))
    implementation(project(":core:core_usecase"))
    implementation(project(":core:core_security"))
    implementation(project(":core:core_monitoring"))

    // Spring Boot 스타터
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Cloud (MSA 기능용)
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // 데이터베이스 (JPA 사용 안할 경우 runtimeOnly 제거 가능)
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // 스케줄링 (CacheCleanupScheduler)
    implementation("net.javacrumbs.shedlock:shedlock-spring:6.6.1")
    implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:6.6.1")

    // 로깅
    implementation("org.slf4j:slf4j-api")

    // 모니터링
    implementation("io.micrometer:micrometer-registry-prometheus")

    // API 문서화 (Swagger/OpenAPI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    // 추가 라이브러리
    implementation("org.hibernate.validator:hibernate-validator")

    // 테스트 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter")
}

kotlin {
    allOpen {
        // annotation("jakarta.persistence.Entity")
        // annotation("jakarta.persistence.MappedSuperclass")
        // annotation("jakarta.persistence.Embeddable")
        annotation("org.springframework.stereotype.Component")
        annotation("org.springframework.stereotype.Service")
        annotation("org.springframework.stereotype.Repository")
        annotation("org.springframework.context.annotation.Configuration")
        annotation("org.springframework.web.bind.annotation.RestController")
        annotation("org.springframework.transaction.annotation.Transactional")
        annotation("org.springframework.scheduling.annotation.Scheduled")
    }
}

sentry {
    includeSourceContext = true
    org = "wildrew-5w"
    projectName = "java-spring-boot"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/config/**",
                        "**/*Application*",
                        "**/*Configuration*",
                        "**/dto/**",
                        "**/model/**",
                        "**/client/response/**",
                        "**/event/model/**",
                        "**/*Constants*",
                        "**/scheduler/**",
                        "**/event/*Publisher*",
                        "**/event/*Consumer*",
                        "**/handler/**",
                    )
                }
            },
        ),
    )
    sourceDirectories.setFrom(files(project.layout.projectDirectory.dir("src/main/kotlin")))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("boot")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
        mavenBom("org.testcontainers:testcontainers-bom:1.21.0")
    }
}
