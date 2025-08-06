import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.time.LocalDate

plugins {
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.spring") version "2.1.0" apply false
    kotlin("plugin.jpa") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("io.sentry.jvm.gradle") version "5.5.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
    id("jacoco")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        verbose.set(true)
        outputToConsole.set(true)
        android.set(false)
        reporters {
            reporter(ReporterType.JSON)
        }
        filter {
            exclude("**/build/generated/**")
        }
    }



    tasks.withType<BootBuildImage>().configureEach {
        val dockerUser = System.getenv("DOCKERHUB_USERNAME") ?: "local"
        val dockerPassword = System.getenv("DOCKERHUB_TOKEN")
            ?: "비밀번호" // 로컬 빌드 대비
        val tag = System.getenv("GITHUB_SHA")?.take(7)
            ?: "dev-${LocalDate.now()}"

        val svcName = project.name
            .removePrefix("jobstat-")
            .replace('_', '-')


        imageName.set("$dockerUser/jobstat-$svcName:$tag")
        publish.set(true)
        docker {
            publishRegistry {
                username.set(dockerUser)
                password.set(dockerPassword)
            }
        }
    }

//    plugins.withType<org.springframework.boot.gradle.plugin.SpringBootPlugin> {
//        println("DEBUG > registry user = '${System.getenv("DOCKERHUB_USERNAME")}'")
//        tasks.withType<BootBuildImage> {
//            publish.set(true)
//        }
//    }

    // --- 이하 다른 공통 설정들 ---
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    apply(plugin = "jacoco")
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.13"
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
}