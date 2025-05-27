import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") // Spring Boot 의존성 및 작업 관리
    id("io.spring.dependency-management") // BOM(Bill of Materials) import를 위함
    kotlin("jvm") // JVM을 위한 Kotlin 지원
    kotlin("plugin.spring") // Kotlin에서 Spring 지원 (예: 클래스를 open으로 만듦)
    kotlin("plugin.jpa") // Kotlin에서 JPA 지원 (예: 엔티티를 위한 no-arg 생성자)
    id("io.sentry.jvm.gradle")
    // KSP와 Kotlin Serialization은 제공된 auth 파일에서 직접적인 사용이 발견되지 않아 생략되었습니다.
    // 만약 이 모듈의 다른 곳에서 MapStruct나 Kotlinx.Serialization을 사용한다면 다음을 추가하세요:
    // kotlin("plugin.serialization")
    // id("com.google.devtools.ksp")
}

group = "com.wildrew" // 또는 이 서비스에 특화된 그룹 (예: com.wildrew.auth)
version = "0.0.1-SNAPSHOT" // 이 서비스의 버전

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24)) // 기존 app 모듈과 동일하게 설정
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core:core_error"))
    implementation(project(":core:core_event"))
    implementation(project(":core:core_jpa_base"))
    implementation(project(":core:core_security"))
    implementation(project(":core:core_token"))
    implementation(project(":core:core_usecase"))
    implementation(project(":core:core_web_util"))
    implementation(project(":core:core_global"))
    implementation(project(":core:core_monitoring"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring Cloud (MSA 기능용)
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    implementation("org.springframework.kafka:spring-kafka")

    // 데이터베이스
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0") // Kotlinx Serialization 사용 시 추가

    // 캐시
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // 모니터링
    implementation("io.micrometer:micrometer-registry-prometheus")

    // SpringDoc OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    // 테스트 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
}

kotlin {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
        annotation("org.springframework.stereotype.Component")
        annotation("org.springframework.stereotype.Service")
        annotation("org.springframework.stereotype.Repository")
        annotation("org.springframework.context.annotation.Configuration")
    }
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Sentry 설정 (기존 app 모듈과 동일)
sentry {
    includeSourceContext.set(true)
    org.set("wildrew-5w")
    projectName.set("java-spring-boot-auth")
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}

tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("boot")
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
                        "**/com/wildrew/app/common/**",
                        "**/entity/**",
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

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
        mavenBom("org.testcontainers:testcontainers-bom:1.21.0")
    }
}
