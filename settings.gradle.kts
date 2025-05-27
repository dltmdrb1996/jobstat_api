plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "jobstat"

include(
    "service:jobstat-auth",
    "service:jobstat-community",
    "service:jobstat-community_read",
    "service:jobstat-notification",
    "service:jobstat-statistics_read",
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
    "infra:jobstat-config-server",
    "infra:jobstat-eureka-server",
    "infra:jobstat-api-gateway",
    ":ksp",
)
include("jobstat-api-gateway")
include("jobstat-auth")
include("jobstat-community")
include("jobstat-community_read")
include("jobstat-notification")
include("jobstat-statistics_read")
