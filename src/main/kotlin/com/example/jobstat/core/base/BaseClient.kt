package com.example.jobstat.core.base

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient

abstract class BaseClient {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    protected abstract fun getServiceUrl(): String

    protected lateinit var restClient: RestClient
        private set // 외부에서 restClient 자체를 변경하는 것은 막음

    @PostConstruct
    fun initRestClient() {
        restClient =
            RestClient
                .builder()
                .baseUrl(getServiceUrl())
                .build()
    }

    protected fun buildUri(
        path: String,
        queryParams: Map<String, Any?> = emptyMap(),
    ): String {
        if (queryParams.isEmpty()) {
            return path
        }
        val builder = StringBuilder(path)
        builder.append('?')
        queryParams.entries
            .filter { it.value != null }
            .forEach { (key, value) ->
                builder
                    .append(key)
                    .append('=')
                    .append(value)
                    .append('&')
            }
        return builder.toString().trimEnd('&')
    }
}
