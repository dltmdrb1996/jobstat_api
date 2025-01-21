plugins {
	kotlin("jvm") version "2.1.0"
}

group = "com.wildrew"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
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


kotlin {
	sourceSets.main {
		kotlin.srcDir("src/main/kotlin")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
