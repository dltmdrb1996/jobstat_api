// core-web-util/build.gradle.kts
plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
    // Spring 관련 어노테이션(@Component 등)을 사용하지 않으므로 kotlin("plugin.spring") 불필요할 수 있음.
    // 단, Pageable 등을 사용하므로 Spring 의존성이 간접적으로 필요.
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations") // @JsonInclude (ApiResponse 공개 필드)
    api("org.springframework:spring-web") // HttpStatus, ResponseEntity (ApiResponse 공개 필드/메소드)
    api("org.springframework.data:spring-data-commons") // Page (ApiResponse<Page<T>> 공개 메소드)

    // 이 모듈은 순수 유틸리티에 가까우므로 slf4j-api는 필요 없을 수 있음. 로깅이 있다면 추가.
    // implementation("org.slf4j:slf4j-api")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
