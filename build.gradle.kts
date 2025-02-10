import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.jpa") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"

//    kotlin("plugin.allopen") version "2.0.10"
//    kotlin("plugin.noarg") version "2.0.10"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("io.sentry.jvm.gradle") version "4.10.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" // ktlint 플러그인 추가

// 	kotlin("plugin.allopen") version "1.9.24" // Add allopen plugin
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
    implementation("org.springframework.boot:spring-boot-starter-actuator")

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

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
    // coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring Boot Mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Additional Libraries
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.0")
//    testImplementation("io.mockk:mockk:1.13.16")

    // 컨테이너 테스트 관련
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:mongodb:1.20.4")
    testImplementation("org.testcontainers:mysql")
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

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
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
