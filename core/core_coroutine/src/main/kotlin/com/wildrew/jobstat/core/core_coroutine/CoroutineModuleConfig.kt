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

    @Bean("coreVirtualThreadExecutorService")
    @ConditionalOnMissingBean(name = ["coreVirtualThreadExecutorService"])
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "true",
        matchIfMissing = false,
    )
    fun virtualThreadExecutorService(): ExecutorService {
        log.info("Creating virtualThreadExecutorService bean.")
        return Executors.newVirtualThreadPerTaskExecutor()
    }

    @Bean("platformThreadExecutorServiceForIo")
    @ConditionalOnMissingBean(name = ["platformThreadExecutorServiceForIo"])
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "false",
        matchIfMissing = true,
    )
    fun platformThreadExecutorServiceForIo(): ExecutorService {
        log.info("Creating platformThreadExecutorServiceForIo bean.")
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    }

    @Bean("coreCoroutineDispatcher")
    @Primary
    @ConditionalOnMissingBean(CoroutineDispatcher::class)
    fun coreCoroutineDispatcher(
        @Value("\${spring.threads.virtual.enabled:false}") virtualThreadsEnabled: Boolean,
        @Qualifier("coreVirtualThreadExecutorService") virtualExecutorService: ExecutorService?,
        @Qualifier("platformThreadExecutorServiceForIo") platformExecutorService: ExecutorService?,
    ): CoroutineDispatcher =
        if (virtualThreadsEnabled && virtualExecutorService != null) {
            log.info("Using virtualThreadCoroutineDispatcher based on coreVirtualThreadExecutorService.")
            virtualExecutorService.asCoroutineDispatcher()
        } else if (platformExecutorService != null) {
            log.info("Using platformThreadCoroutineDispatcher based on platformThreadExecutorServiceForIo.")
            platformExecutorService.asCoroutineDispatcher()
        } else {
            Dispatchers.IO
        }

    @Bean("coreCoroutineScope")
    @Primary
    @ConditionalOnMissingBean(CoroutineScope::class)
    fun coreCoroutineScope(
        coroutineDispatcher: CoroutineDispatcher,
    ): CoroutineScope {
        log.info("Creating coreCoroutineScope bean with dispatcher: {}", coroutineDispatcher)
        return CoroutineScope(coroutineDispatcher + SupervisorJob())
    }
}
