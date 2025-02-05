package com.example.jobstat.core.security.annotation

import org.springframework.security.access.prepost.PreAuthorize

/**
 * 공개 접근이 가능한 API를 표시하는 어노테이션
 * 인증이 필요하지 않은 엔드포인트에 사용됩니다
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("permitAll()")
annotation class Public
