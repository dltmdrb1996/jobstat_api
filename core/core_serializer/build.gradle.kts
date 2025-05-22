// core-serializer/build.gradle.kts
plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
    // kotlin("plugin.serialization") // kotlinx.serialization 사용 시, 여기서는 Jackson 사용
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")         // DataSerializer, ObjectMapper 빈이 사용/반환
    api("com.fasterxml.jackson.module:jackson-module-kotlin") // ObjectMapper 구성
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310") // ObjectMapper 구성 (JavaTimeModule)
    api("com.fasterxml.jackson.module:jackson-module-afterburner") // ObjectMapper 구성 (성능 향상)

    implementation("org.slf4j:slf4j-api")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure") // @AutoConfiguration
    compileOnly("org.springframework:spring-beans")      // @Bean
    compileOnly("org.springframework:spring-context")    // @Configuration
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}