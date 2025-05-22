// core-jpa-base/build.gradle.kts
plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.jpa") // JPA 엔티티 사용
    id("io.spring.dependency-management")
    kotlin("plugin.spring") // Spring Data JPA, Auditing 등
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api("jakarta.persistence:jakarta.persistence-api")        // Base 엔티티, SnowflakeIdGenerator 등이 사용
    api("org.springframework.data:spring-data-jpa")        // Auditing 관련 어노테이션/리스너, JpaRepository
    api("org.hibernate.orm:hibernate-core")                // @IdGeneratorType, IdentifierGenerator (SnowflakeIdGenerator)

    implementation("org.slf4j:slf4j-api")                 // SnowflakeGenerator 내부 로깅

    compileOnly("org.springframework.boot:spring-boot-autoconfigure") // @AutoConfiguration
    compileOnly("org.springframework:spring-beans")      // @Bean, @Value 등
    compileOnly("org.springframework:spring-context")    // @Configuration
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
}

kotlin {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
    }
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}