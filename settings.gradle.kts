rootProject.name = "jobstat"

include(
    ":app", // 애플리케이션 모듈 (기존 main 코드가 이동될 곳)
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
    ":ksp",
)
// // 기존 ksp 모듈이 있다면 유지
// if (file("ksp").exists()) {
//    include(":ksp")
// }
