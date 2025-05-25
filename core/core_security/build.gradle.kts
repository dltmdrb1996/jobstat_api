// core-security/build.gradle.kts
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
    api(project(":core:core_error")) // 공개 클래스/메소드가 AppException 사용
    api(project(":core:core_token")) // 공개 클래스/메소드가 JwtTokenParser 등 사용
    api(project(":core:core_web_util")) // 공개 에러 핸들러가 ApiResponse 사용

    api("org.springframework.boot:spring-boot-starter-security") // 보안 설정을 제공 (SecurityFilterChain 등)
    api("org.springframework:spring-webmvc") // RequestMappingHandlerMapping (필터 빈의 생성자 파라미터)

    implementation(project(":core:core_serializer")) // JwtAuthenticationEntryPoint 내부 ObjectMapper 사용
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0") // 필터 내부 캐시 구현용
    implementation("org.slf4j:slf4j-api")
    implementation("org.aspectj:aspectjrt") // RateLimitAspect 런타임용

    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.aspectj:aspectjweaver") // AOP 컴파일용
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
