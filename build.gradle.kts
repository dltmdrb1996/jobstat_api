import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.jpa") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("io.sentry.jvm.gradle") version "5.5.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" // ktlint 플러그인 추가
    id("jacoco")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21 // 변경
    targetCompatibility = JavaVersion.VERSION_21 // 변경
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24)) // 유지 (JDK 24 사용 명시)
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

sentry {

    includeSourceContext = true

    org = "wildrew-5w"
    projectName = "java-spring-boot"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

dependencies {
    // Spring Boot Starter
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka:3.3.4")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("com.h2database:h2") // For in-memory database
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("redis.clients:jedis")

    // ksp
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.0-1.0.29")
    ksp(project(":ksp"))

    // Spring Boot DevTools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Optional - SLF4J API for Logging
    implementation("org.slf4j:slf4j-api")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")

    // monitoring
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.5")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")
//    implementation("org.springdoc:springdoc-openapi-starter-common:2.8.4")

//    implementation("org.springdoc:springdoc-openapi-kotlin:2.8.4")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.18.3")

    // Spring Boot Mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Additional Libraries
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("io.jsonwebtoken:jjwt-api:0.12.5") // 최신 버전 확인
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.0")
//    testImplementation("io.mockk:mockk:1.13.16")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.2.1") // 실제 최신 버전 확인 필요

    // 컨테이너 테스트 관련
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:mongodb:1.20.4")
    testImplementation("org.testcontainers:mysql")

    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
}

kotlin {
    // allOpen plugin configurations for JPA annotations
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
        annotation("org.springframework.data.mongodb.core.mapping.Document") // MongoDB Document 추가
    }
}

tasks.withType<Test> {
    useJUnitPlatform() // Enables JUnit 5 platform for tests
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

ktlint {
    version.set("1.5.0")
    verbose.set(true)
    outputToConsole.set(true)
    android.set(false)
    reporters {
        reporter(ReporterType.JSON)
    }

    filter {
        exclude("**/build/generated/**")
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // 테스트 실행 후 리포트 생성
    reports {
        xml.required.set(true) // SonarQube나 다른 도구와 통합 시 필요
        html.required.set(true) // 브라우저에서 볼 수 있는 HTML 리포트
        csv.required.set(false) // CSV 리포트는 필요 없는 경우
    }

    // 커버리지 측정에서 제외할 클래스 지정 (필요한 경우)
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
}

// tasks.register("checkInternalModifier") {
//    doLast {
//        val sourceDir = project.projectDir.resolve("src/main/kotlin")
//        var hasError = false
//
//        sourceDir.walk()
//            .filter { it.isFile && it.extension == "kt" }
//            .forEach { file ->
//                val content = file.readText()
//                val packageLine = content.lines().find { it.startsWith("package") }
//
//                // domain 패키지에 있는 파일만 검사
//                if (packageLine?.contains(".domain.") == true) {
//                    // 클래스/데이터 클래스 선언을 찾음
//                    val classDeclarations = content.lines().filter { line ->
//                        (line.contains("class ") || line.contains("data class ")) &&
//                                !line.contains("interface") && // 인터페이스 제외
//                                !line.contains("Service") // Service 인터페이스 제외
//                    }
//
//                    classDeclarations.forEach { declaration ->
//                        if (!declaration.contains("internal")) {
//                            println("Error: Missing internal modifier in ${file.name}: $declaration")
//                            hasError = true
//                        }
//                    }
//                }
//            }
//
//        if (hasError) {
//            throw GradleException("Found classes without internal modifier in domain package")
//        }
//    }
// }
