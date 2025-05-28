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
    api(project(":core:core_error"))

    api("io.jsonwebtoken:jjwt-api:0.12.5")
    api("io.jsonwebtoken:jjwt-impl:0.12.5")
    api("io.jsonwebtoken:jjwt-jackson:0.12.5")

    implementation("org.slf4j:slf4j-api")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-beans")
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
