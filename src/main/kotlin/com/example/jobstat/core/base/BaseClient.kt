package com.example.jobstat.core.base

import com.example.jobstat.core.global.wrapper.ApiResponse
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.util.concurrent.ConcurrentHashMap

/**
 * 마이크로서비스 통신을 위한 기본 HTTP 클라이언트
 * 공통 기능을 제공하고 타입 안전성을 보장
 */
abstract class BaseClient {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 마이크로서비스의 기본 URL을 반환
     */
    protected abstract fun getServiceUrl(): String
    
    protected lateinit var restClient: RestClient
    
    // 파라미터화된 타입 레퍼런스를 캐싱하여 재사용
    private val typeReferenceCache = ConcurrentHashMap<Class<*>, ParameterizedTypeReference<*>>()
    
    @PostConstruct
    fun initRestClient() {
        restClient = RestClient.builder()
            .baseUrl(getServiceUrl())
            .build()
    }
    
    /**
     * GET 요청 실행
     */
    protected fun <T : Any> executeGet(
        uri: String,
        responseType: Class<T>,
        logContext: String
    ): T? {
        return executeRequest(
            responseType,
            logContext
        ) { restClient.get().uri(uri) }
    }
    
    /**
     * POST 요청 실행
     */
    protected fun <T : Any> executePost(
        uri: String,
        body: Any,
        responseType: Class<T>,
        logContext: String
    ): T? {
        return executeRequest(
            responseType,
            logContext,
        ) { restClient.post().uri(uri).body(body) }
    }
    
    /**
     * PUT 요청 실행
     */
    protected fun <T : Any> executePut(
        uri: String,
        body: Any,
        responseType: Class<T>,
        logContext: String
    ): T? {
        return executeRequest(
            responseType,
            logContext,
        ) { restClient.put().uri(uri).body(body) }
    }
    
    /**
     * DELETE 요청 실행
     */
    protected fun <T : Any> executeDelete(
        uri: String,
        responseType: Class<T>,
        logContext: String
    ): T? {
        return executeRequest(
            responseType,
            logContext,
        ) { restClient.delete().uri(uri) }
    }
    
    /**
     * API 요청 실행을 위한 통합 메서드
     * 예외 처리 및 로깅 일원화
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> executeRequest(
        responseType: Class<T>,
        logContext: String,
        requestSpecSupplier: () -> RestClient.RequestHeadersSpec<*>,
    ): T? {
        return try {
            val typeRef = getTypeReference(responseType) as ParameterizedTypeReference<ApiResponse<T>>
            val response = requestSpecSupplier.invoke()
                .retrieve()
                .body(typeRef)
            
            response?.data
        } catch (e: HttpClientErrorException) {
            log.error("[{}] Client error: {}, Status: {}", logContext, e.message, e.statusCode, e)
            null
        } catch (e: HttpServerErrorException) {
            log.error("[{}] Server error: {}, Status: {}", logContext, e.message, e.statusCode, e)
            null
        } catch (e: ResourceAccessException) {
            log.error("[{}] Network error: {}", logContext, e.message, e)
            null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error: {}", logContext, e.message, e)
            null
        }
    }
    
    /**
     * 타입 참조 객체 생성 또는 캐시에서 검색
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getTypeReference(clazz: Class<T>): ParameterizedTypeReference<*> {
        return typeReferenceCache.computeIfAbsent(clazz) {
            object : ParameterizedTypeReference<ApiResponse<T>>() {}
        }
    }
    
    /**
     * URI 빌더 유틸리티
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