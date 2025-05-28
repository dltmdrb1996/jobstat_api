import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

group = "com.wildrew"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    sourceSets.main {
        kotlin.srcDir("src/main/kotlin")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.0-1.0.29")
    implementation("com.squareup:kotlinpoet:1.18.1")
    implementation("com.squareup:kotlinpoet-metadata:1.18.1")
    implementation("com.squareup:kotlinpoet-ksp:1.18.1")

//    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // Spring Data MongoDB (버전 업데이트)
    implementation("org.springframework.data:spring-data-mongodb:4.2.2")

    // MongoDB Java Driver (버전 업데이트)
    implementation("org.mongodb:mongodb-driver-sync:5.0.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
