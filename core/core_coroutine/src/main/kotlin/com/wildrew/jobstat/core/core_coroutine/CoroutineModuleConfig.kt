package com.wildrew.jobstat.core.core_coroutine

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AutoConfiguration
class CoroutineModuleConfig {

    private val log = LoggerFactory.getLogger(CoroutineModuleConfig::class.java)

    // ExecutorService 빈들은 이전과 같이 조건부로 생성될 수 있음
    @Bean("coreVirtualThreadExecutorService")
    @ConditionalOnMissingBean(name = ["coreVirtualThreadExecutorService"])
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    fun virtualThreadExecutorService(): ExecutorService {
        log.info("Creating virtualThreadExecutorService bean.")
        return Executors.newVirtualThreadPerTaskExecutor()
    }

    @Bean("platformThreadExecutorServiceForIo") // 필요하다면 IO 작업용 플랫폼 스레드 풀
    @ConditionalOnMissingBean(name = ["platformThreadExecutorServiceForIo"])
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "false",
        matchIfMissing = true // 가상 스레드 비활성화 시 기본으로 생성
    )
    fun platformThreadExecutorServiceForIo(): ExecutorService {
        log.info("Creating platformThreadExecutorServiceForIo bean.")
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    }


    @Bean("coreCoroutineDispatcher") // 단일 CoroutineDispatcher 빈
    @Primary
    @ConditionalOnMissingBean(CoroutineDispatcher::class)
    fun coreCoroutineDispatcher(
        @Value("\${spring.threads.virtual.enabled:false}") virtualThreadsEnabled: Boolean,
        // Optional하게 ExecutorService 주입 (존재할 수도, 안 할 수도 있음)
        @Qualifier("coreVirtualThreadExecutorService") virtualExecutorService: ExecutorService?,
        @Qualifier("platformThreadExecutorServiceForIo") platformExecutorService: ExecutorService?
    ): CoroutineDispatcher {
        return if (virtualThreadsEnabled && virtualExecutorService != null) {
            log.info("Using virtualThreadCoroutineDispatcher based on coreVirtualThreadExecutorService.")
            virtualExecutorService.asCoroutineDispatcher()
        } else if (platformExecutorService != null) {
            log.info("Using platformThreadCoroutineDispatcher based on platformThreadExecutorServiceForIo.")
            platformExecutorService.asCoroutineDispatcher()
        }
        else {
            Dispatchers.IO // 최후의 수단 또는 기본값
        }
    }

    @Bean("coreCoroutineScope") // 단일 CoroutineScope 빈
    @Primary
    @ConditionalOnMissingBean(CoroutineScope::class)
    fun coreCoroutineScope(
        // 위에서 정의된 coreCoroutineDispatcher 빈을 주입받음
        // @Qualifier("coreCoroutineDispatcher")를 명시할 수도 있지만, Primary 빈이므로 자동 주입 가능성 높음
        coroutineDispatcher: CoroutineDispatcher
    ): CoroutineScope {
        log.info("Creating coreCoroutineScope bean with dispatcher: {}", coroutineDispatcher)
        return CoroutineScope(coroutineDispatcher + SupervisorJob())
    }
}