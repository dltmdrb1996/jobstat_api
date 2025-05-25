import org.springframework.boot.gradle.tasks.bundling.BootJar

// app/build.gradle.kts
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management") // 버전은 루트에서 가져옴
    kotlin("jvm") // 버전은 루트에서 가져옴
    kotlin("plugin.spring") // 버전은 루트에서 가져옴
    kotlin("plugin.jpa") // 버전은 루트에서 가져옴
    kotlin("plugin.serialization") // 버전은 루트에서 가져옴
    id("com.google.devtools.ksp") // 버전은 루트에서 가져옴
    id("io.sentry.jvm.gradle") // 버전은 루트에서 가져옴
}

group = "com.wildrew"
version = "0.0.1-SNAPSHOT"

// Java 버전 설정 (app 모듈에만 적용)
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

dependencies {
    // Core Library Modules
    implementation(project(":core:core_coroutine"))
    implementation(project(":core:core_error"))
    implementation(project(":core:core_event"))
    implementation(project(":core:core_jpa_base"))
    implementation(project(":core:core_monitoring"))
    implementation(project(":core:core_open_api"))
    implementation(project(":core:core_security"))
    implementation(project(":core:core_serializer"))
    implementation(project(":core:core_token"))
    implementation(project(":core:core_usecase"))
    implementation(project(":core:core_web_util"))
    implementation(project(":core:core_global"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.lettuce:lettuce-core")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    // ksp
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    if (project.rootProject.file("ksp").exists()) {
        ksp(project(":ksp"))
    }

    implementation("org.slf4j:slf4j-api")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Boot Mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Additional Libraries
    implementation("org.hibernate.validator:hibernate-validator")

    // OpenFeign (버전은 BOM으로 관리)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // BouncyCastle
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")

    // SpringDoc OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4") // 또는 최신 버전

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    ksp("org.mapstruct:mapstruct-processor:1.6.3")

    // Test Dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0") // 최신 안정 버전 확인
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Testcontainers (버전은 BOM으로 관리)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.testcontainers:mysql")
}

sentry {
    includeSourceContext = true
    org = "wildrew-5w"
    projectName = "java-spring-boot"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

kotlin {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
        annotation("org.springframework.data.mongodb.core.mapping.Document")
    }
}

tasks.named("generateSentryBundleIdJava") {
    dependsOn("kspKotlin")
}
tasks.named("sentryCollectSourcesJava") {
    dependsOn("kspKotlin")
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
                    )
                }
            },
        ),
    )
    sourceDirectories.setFrom(files(project.layout.projectDirectory.dir("src/main/kotlin")))
}

tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("boot")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        // Spring Cloud BOM은 Spring Boot 버전에 맞는 것을 사용해야 합니다.
        // 예를 들어 Spring Boot 3.2.x는 Spring Cloud 2023.0.x 버전과 호환됩니다.
        // 사용하시는 Spring Boot 3.4.5 버전에 맞는 Spring Cloud BOM 버전을 확인해주세요.
        // 여기서는 예시로 2023.0.3을 사용했지만, 실제로는 다를 수 있습니다.
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1") // 버전 확인 필요!
        mavenBom("org.testcontainers:testcontainers-bom:1.21.0")
    }
}
