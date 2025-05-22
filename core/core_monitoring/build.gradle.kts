// core-monitoring/build.gradle.kts
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
    api("io.micrometer:micrometer-core") // MeterRegistryCustomizer 빈이 MeterRegistry, Tag 등 사용

    implementation("io.sentry:sentry:8.12.0")                 // 내부 Sentry 로직 (CoreMonitoringAutoConfiguration)
    implementation("ch.qos.logback:logback-classic") // 내부 Logback 로직 (CoreMonitoringAutoConfiguration)
    implementation("org.slf4j:slf4j-api")

    compileOnly("io.micrometer:micrometer-registry-prometheus") // 실제 프로메테우스 연동은 app에서
    compileOnly("org.springframework.boot:spring-boot-starter-actuator") // AutoConfiguration 작성에 필요
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-context")  // @Scheduled, Environment 등
    compileOnly("org.springframework:spring-beans")    // @Value
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}