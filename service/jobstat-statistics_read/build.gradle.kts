import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("com.google.devtools.ksp")
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

dependencies {
    // Core Library Modules (제공된 파일에서 실제 사용이 확인된 모듈)
    implementation(project(":core:core_global"))
    implementation(project(":core:core_usecase"))
    implementation(project(":core:core_error"))
    implementation(project(":core:core_monitoring"))
    implementation(project(":core:core_open_api"))
    implementation(project(":core:core_security"))
    implementation(project(":core:core_web_util"))
//    implementation(project(":core:core_jpa_base"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//
    // Spring Cloud (MSA 환경을 위한 기본 구성 요소)
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    if (project.rootProject.file("ksp").exists()) {
        ksp(project(":ksp"))
    }

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")

    // SpringDoc OpenAPI (Swagger 어노테이션 사용)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    // Test Dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mongodb")
}

sentry {
    includeSourceContext = true
    org = "wildrew-5w"
    projectName = "java-spring-boot"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

kotlin {
    allOpen {
        annotation("jakarta.persistence.Embeddable")
        annotation("org.springframework.data.mongodb.core.mapping.Document")
    }
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
                        "**/core/core_model/**",
                        "**/core/core_mongo_base/model/**",
                        "**/stats/document/**",
                        "**/stats/registry/*Type.kt",
                        "**/rankings/document/**",
                        "**/rankings/model/**",
                        "**/rankings/model/rankingtype/**",
                        "**/usecase/**/Request.class",
                        "**/usecase/**/Response.class",
                        "**/*Controller.class",
                    )
                }
            },
        ),
    )
    sourceDirectories.setFrom(files(project.layout.projectDirectory.dir("src/main/kotlin")))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.named("generateSentryBundleIdJava") {
    dependsOn("kspKotlin")
}

tasks.named("sentryCollectSourcesJava") {
    dependsOn("kspKotlin")
}

tasks.named("sentryCollectSourcesJava") {
    dependsOn("kspTestKotlin")
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
