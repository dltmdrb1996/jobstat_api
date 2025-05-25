import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

group = "com.wildrew"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-config-server")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
    }
}


tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("boot")
}

val copyConfigRepo = tasks.register<Copy>("copyConfigRepo") {
    from(layout.projectDirectory.dir("../config-repo"))
    into(layout.buildDirectory.dir("resources/main/config-repo"))
}

tasks.named("processResources") {
    dependsOn(copyConfigRepo)
}