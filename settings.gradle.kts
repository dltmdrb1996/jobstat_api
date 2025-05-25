plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "jobstat"

include(
    ":app",
    "core:core_coroutine",
    "core:core_error",
    "core:core_event",
    "core:core_jpa_base",
    "core:core_monitoring",
    "core:core_open_api",
    "core:core_security",
    "core:core_serializer",
    "core:core_token",
    "core:core_usecase",
    "core:core_web_util",
    "core:core_global",
    ":jobstat-config-server",
    ":jobstat-eureka-server",
    ":ksp",
)
