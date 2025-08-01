import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway") // Spring Cloud Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client") // Eureka Client (서비스 디스커버리용)
    implementation("org.springframework.cloud:spring-cloud-starter-config") // Config Server로부터 설정 로드
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // JWT 토큰 파싱을 위해 core_token 모듈 의존성 추가
    implementation(project(":core:core_token"))
    implementation(project(":core:core_error"))
    implementation(project(":core:core_web_util"))
    implementation(project(":core:core_serializer"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test") // WebFlux 테스트용
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
