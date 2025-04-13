package com.example.jobstat.core.base

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient

/**
 * 마이크로서비스 통신을 위한 기본 HTTP 클라이언트 (간소화 버전)
 * - 설정된 RestClient 인스턴스 제공
 * - 공통 유틸리티 메소드 제공 (예: buildUri)
 */
abstract class BaseClient {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 마이크로서비스의 기본 URL을 반환
     */
    protected abstract fun getServiceUrl(): String

    /**
     * 하위 클라이언트에서 직접 사용할 수 있도록 protected로 변경
     */
    protected lateinit var restClient: RestClient
        private set // 외부에서 restClient 자체를 변경하는 것은 막음

    @PostConstruct
    fun initRestClient() {
        // RestClient 초기화는 유지
        restClient = RestClient.builder()
            .baseUrl(getServiceUrl())
            .build()
    }

    /**
     * URI 빌더 유틸리티 (유지)
     */
    protected fun buildUri(path: String, queryParams: Map<String, Any?> = emptyMap()): String {
        if (queryParams.isEmpty()) {
            return path
        }
        val builder = StringBuilder(path)
        builder.append('?')
        queryParams.entries
            .filter { it.value != null }
            .forEach { (key, value) ->
                builder.append(key).append('=').append(value).append('&')
            }
        return builder.toString().trimEnd('&')
    }
}