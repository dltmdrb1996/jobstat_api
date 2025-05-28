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
    api(project(":core:core_web_util"))

    api("org.springframework:spring-web")
    api("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.springframework.data:spring-data-jpa")
    implementation("org.hibernate.orm:hibernate-core")
    implementation("jakarta.validation:jakarta.validation-api")

    implementation("org.slf4j:slf4j-api")
    implementation("io.sentry:sentry:8.12.0")

    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework:spring-beans")
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
