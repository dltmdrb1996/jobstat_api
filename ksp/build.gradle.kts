import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm") version "2.1.0"
}

group = "com.wildrew"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21 // 루트와 일치 또는 ksp 모듈 특성에 맞게
    targetCompatibility = JavaVersion.VERSION_21 // 루트와 일치 (중요)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // 루트와 일치 (빌드/실행 JDK)
    }
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21) // !!! 매우 중요: 루트 모듈과 jvmTarget 일치 !!!
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    sourceSets.main { // 이 블록은 기존대로 유지 가능 (Kotlin 소스 디렉토리 지정)
        kotlin.srcDir("src/main/kotlin")
    }
}

dependencies {
	implementation("com.google.devtools.ksp:symbol-processing-api:2.1.0-1.0.29")
	implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
	implementation("com.squareup:kotlinpoet:1.18.1")  // 버전 변경
	implementation("com.squareup:kotlinpoet-metadata:1.18.1")  // 버전 변경
	implementation("com.squareup:kotlinpoet-ksp:1.18.1")  // KSP 지원을 위해 추가
	implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")

	// Spring Data MongoDB (버전 업데이트)
	implementation("org.springframework.data:spring-data-mongodb:4.2.2")

	// MongoDB Java Driver (버전 업데이트)
	implementation("org.mongodb:mongodb-driver-sync:5.0.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
