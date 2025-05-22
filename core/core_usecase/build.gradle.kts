// core-usecase/build.gradle.kts
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
    implementation("jakarta.validation:jakarta.validation-api") // ValidUseCase 생성자, 예외, DTO 어노테이션용
    implementation("org.springframework:spring-tx")             // @Transactional (TransactionalValidUseCase)
    compileOnly("org.slf4j:slf4j-api")
    implementation("org.aspectj:aspectjrt")         // @LoggedUseCase Aspect 런타임용

    compileOnly("org.aspectj:aspectjweaver") // AOP 컴파일용
    compileOnly("org.springframework.boot:spring-boot-autoconfigure") // @AutoConfiguration
    compileOnly("org.springframework:spring-beans")      // @Bean
    compileOnly("org.springframework:spring-context")    // @Configuration
    compileOnly("org.springframework:spring-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}