plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.jpa")
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
    api(project(":core:core_serializer"))
    api(project(":core:core_error"))
    api(project(":core:core_jpa_base"))
    api(project(":core:core_coroutine"))
    implementation(project(":core:core_global"))

    api("org.springframework.kafka:spring-kafka")

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

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
        annotation("jakarta.persistence.Entity")
    }
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
