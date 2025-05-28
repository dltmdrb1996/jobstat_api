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
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

dependencies {
    api(project(":core:core_error"))
    api(project(":core:core_token"))
    api(project(":core:core_web_util"))

    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework:spring-webmvc")

    implementation(project(":core:core_serializer"))
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
    implementation("org.slf4j:slf4j-api")
    implementation("org.aspectj:aspectjrt")

    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.aspectj:aspectjweaver")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
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
