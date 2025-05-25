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
    implementation(project(":core:core_monitoring")) // 서비스에 좋은 관행

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis") // TokenService용
    implementation("org.springframework.boot:spring-boot-starter-cache") // LoginAttemptService용
    implementation("org.springframework.boot:spring-boot-starter-actuator") // 모니터링용
    implementation("org.springframework.boot:spring-boot-starter-mail") // UserEventPublisher 이메일 발송용

    // Spring Cloud (MSA 기능용)
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    // implementation("org.springframework.cloud:spring-cloud-starter-openfeign") // 이 서비스가 Feign을 통해 다른 서비스를 호출하는 경우 추가

    // 메시징 (UserEventPublisher가 발행하는 이벤트를 위해 Kafka를 사용한다고 가정)
    implementation("org.springframework.kafka:spring-kafka")

    // 데이터베이스
    runtimeOnly("com.mysql:mysql-connector-j") // 또는 운영 환경 데이터베이스 커넥터
    runtimeOnly("com.h2database:h2") // 로컬 개발/테스트용

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0") // Kotlinx Serialization 사용 시 추가

    // 캐시
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0") // spring-boot-starter-cache의 기본 캐시 구현체

    // 모니터링
    implementation("io.micrometer:micrometer-registry-prometheus") // Prometheus 메트릭용

    // SpringDoc OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4") // 최신 버전 확인

    // MapStruct (이 모듈에서 MapStruct를 사용한 DTO 매핑 시 추가)
    // implementation("org.mapstruct:mapstruct:1.6.3")
    // ksp("org.mapstruct:mapstruct-processor:1.6.3") // KSP 플러그인 필요

    // 테스트 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0") // 또는 최신 버전
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter") // 컨테이너를 사용한 통합 테스트용
    testImplementation("org.testcontainers:mysql") // MySQL 통합 테스트용
}

kotlin {
    allOpen {
        // JPA 엔티티 및 기타 Spring 어노테이션이 붙은 클래스가 open 상태인지 확인
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
        annotation("org.springframework.stereotype.Component")
        annotation("org.springframework.stereotype.Service")
        annotation("org.springframework.stereotype.Repository")
        annotation("org.springframework.context.annotation.Configuration")
        // annotation("org.springframework.data.mongodb.core.mapping.Document") // MongoDB 문서 사용 시
    }
    // 필요한 경우 컴파일러 인자 추가 (예: JPA no-arg 생성자)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        // freeCompilerArgs.add("-Xjsr305=strict") // 예시
    }
}

// Sentry 설정 (기존 app 모듈과 동일)
sentry {
    includeSourceContext.set(true)
    org.set("wildrew-5w") // 기존 설정에서 가져옴
    projectName.set("java-spring-boot-auth") // 이 서비스에 맞는 특정 프로젝트 이름 고려
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN"))
    // 버전이 설정되어 있는지 확인, 그렇지 않으면 Sentry Gradle 플러그인 기본값을 사용할 수 있음
    // setTag("version", project.version.toString()) // 버전 태그 설정 예시
}

// KSP를 사용하지 않는 경우, Sentry 및 KSP와 관련된 이 작업들은 필요하지 않거나
// 명시적인 dependsOn이 필요 없을 수 있습니다.
// tasks.named("generateSentryBundleIdJava") {
//    dependsOn("kspKotlin") // KSP가 활성화되어 있고 관련된 경우에만
// }
// tasks.named("sentryCollectSourcesJava") {
//    dependsOn("kspKotlin") // KSP가 활성화되어 있고 관련된 경우에만
// }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}

tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("boot") // Spring Boot 실행 가능 jar의 표준
    // mainClass.set("com.wildrew.app.AuthServiceApplicationKt") // 자동 감지되지 않는 경우 메인 클래스 설정
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // 보고서 생성 전 테스트 실행 보장
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    // 필요에 따라 설정, DTO, 애플리케이션 메인 클래스 등 제외
                    exclude(
                        "**/config/**",
                        "**/*Application*",
                        "**/*Configuration*",
                        "**/dto/**", // 일반 DTO는 커버리지에서 덜 중요할 수 있음
                        "**/com/wildrew/app/common/**", // 유틸리티 클래스는 다른 곳에서 광범위하게 테스트되거나 간단한 경우 제외 가능
                        "**/entity/**" // 엔티티는 종종 단순 데이터 홀더, getter/setter 임
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

// 의존성 관리 - 버전은 일반적으로 루트 build.gradle.kts에서 관리됩니다.
// 이 블록은 버전 정보를 어디서 찾을지(BOM - Bill of Materials) 정의합니다.
dependencyManagement {
    imports {
        // Spring Boot BOM
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        // Spring Cloud BOM - Spring Boot 버전과의 호환성 확인
        // Spring Boot 3.2.x의 경우, Spring Cloud 2023.0.x가 일반적입니다.
        // (원본의) Spring Boot 3.4.5의 경우, 더 새로운 Spring Cloud BOM이 필요합니다.
        // 원본은 2024.0.1을 사용했으며, 이는 최신 Spring Boot 3.x와 일치합니다.
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1") // 또는 사용 중인 Boot 버전에 맞는 버전
        // Testcontainers BOM
        mavenBom("org.testcontainers:testcontainers-bom:1.21.0") // 또는 최신 버전
    }
}