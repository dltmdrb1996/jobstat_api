// 루트 프로젝트의 build.gradle.kts
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    // 여기서는 플러그인 버전을 중앙에서 관리하고, 각 모듈에서는 ID만 사용하도록 `apply false`를 유지합니다.
    // 단, `org.springframework.boot`는 :app 모듈에서만 사용하므로 여기서는 제외하거나 `apply false`로 둡니다.
    id("org.springframework.boot") version "3.4.5" apply false // <--- 여기에 apply false 추가!
    id("io.spring.dependency-management") version "1.1.7" apply false // <--- 여기도 apply false 추가!
    kotlin("jvm") version "2.1.0" apply false // <--- 다른 플러그인들도 apply false 고려
    kotlin("plugin.spring") version "2.1.0" apply false
    kotlin("plugin.jpa") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("io.sentry.jvm.gradle") version "5.5.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" // 모든 모듈에 적용
    id("jacoco") // 모든 모듈에 적용
}

group = "com.example" // 이전 `com.wildrew`에서 `com.example`로 변경 (제공된 정보 기준)
version = "0.0.1-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        verbose.set(true)
        outputToConsole.set(true)
        android.set(false) // ktlint-gradle 플러그인의 android 프로퍼티는 boolean 타입
        reporters {
            reporter(ReporterType.JSON)
        }
        filter {
            exclude("**/build/generated/**")
        }
    }
}

subprojects {
    // 각 모듈에서 kotlin("jvm")을 apply하고, 버전은 여기서 관리됩니다.
    // apply(plugin = "kotlin") // kotlin("jvm")으로 대체
    // apply(plugin = "kotlin-kapt") // ksp를 사용하므로 제거 또는 필요한 모듈에서만

    // 공통 Kotlin 컴파일러 옵션
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    // 공통 Jacoco 설정
    apply(plugin = "jacoco")
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // `java` 블록 설정은 각 모듈의 build.gradle.kts로 이동합니다.
    // subprojects 에서는 플러그인이 적용되었다는 보장이 없기 때문입니다.
}