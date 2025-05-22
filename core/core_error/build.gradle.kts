// core-error/build.gradle.kts
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
    api(project(":core:core_web_util")) // ApiResponse 사용

    api("org.springframework:spring-web") // HttpStatus, @ExceptionHandler, ResponseEntity
    api("org.springframework.boot:spring-boot-autoconfigure") // @AutoConfiguration

    // ExceptionHandlers.kt 내부 구현에 사용 (공개 API에는 노출 안 됨)
    implementation("org.springframework.data:spring-data-jpa") // DataIntegrityViolationException 등
    implementation("org.hibernate.orm:hibernate-core") // org.hibernate.exception.ConstraintViolationException
    implementation("jakarta.validation:jakarta.validation-api") // ConstraintViolationException (from jakarta.validation)

    implementation("org.slf4j:slf4j-api")
    implementation("io.sentry:sentry:8.12.0") // Sentry SDK 직접 사용

    compileOnly("org.springframework:spring-webmvc") // DispatcherServlet, NoHandlerFoundException 등 포함
    compileOnly("jakarta.servlet:jakarta.servlet-api") // HttpServletRequest (provided)
    compileOnly("org.springframework:spring-beans")    // @Value 등
    compileOnly("org.springframework:spring-context")  // Environment 등
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test") // 테스트 환경 구성
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}