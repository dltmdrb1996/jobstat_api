// core-event/build.gradle.kts
plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.jpa") // Outbox, DLT Entity
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
    api(project(":core:core_serializer")) // DataSerializer, Event<T>.toJson, ObjectMapper
    api(project(":core:core_error"))     // AppException
    api(project(":core:core_jpa_base"))  // AuditableEntitySnow, SnowflakeGenerator
    api(project(":core:core_coroutine")) // OutboxMessageRelay 에서 CoroutineScope 사용
    implementation(project(":core:core_global"))

    api("org.springframework.kafka:spring-kafka") // Kafka 관련 빈(ContainerFactory, KafkaTemplate) 제공
    // ObjectMapper는 core-serializer를 통해 api로 이미 제공됨. DLTConsumer 등이 주입받아 사용.
    // 추가적인 Jackson 의존성이 직접 필요하다면 implementation으로 추가 (예: 특정 모듈)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") // OutboxRepository 등 사용위해

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-beans")
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

kotlin {
    allOpen {
        annotation("jakarta.persistence.Entity") // Outbox, DeadLetterTopicEvent
    }
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}