package com.wildrew.jobstat.community // 패키지 경로는 실제 위치에 맞게 수정하세요.

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Component
@RestController
class SecurityStrategyLogger {

    // Kotlin 스타일의 간결한 로거 선언
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 애플리케이션이 시작될 때 현재 SecurityContextHolder 전략을 로그로 출력합니다.
     * @EventListener는 Spring Boot가 애플리케이션 시작 시점에 이 메서드를 호출하도록 합니다.
     * 이 메서드는 API 호출 없이도 서버 로그에 전략을 출력하는 역할을 합니다.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun logSecurityStrategyOnStartup() {
        // 현재 설정된 전략 객체를 가져옵니다.
        val strategy: SecurityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy()

        log.info("======================================================================")
        log.info("가상 쓰레드 활성화 여부: {}", if (Thread.currentThread().isVirtual) "활성화" else "비활성화")
        log.info("애플리케이션 시작! 현재 활성화된 SecurityContextHolder 전략: {}", strategy.javaClass.name)
        log.info("======================================================================")
    }

    /**
     * API 요청을 통해 현재 전략을 실시간으로 확인할 수 있는 디버깅용 엔드포인트입니다.
     * @return 현재 전략 클래스의 이름
     */
    @PreAuthorize("permitAll()")
    @GetMapping("/debug/security-strategy")
    fun getSecurityStrategy(): String {
        val currentThread = Thread.currentThread()
        val strategyName = SecurityContextHolder.getContextHolderStrategy().javaClass.name
        log.info("======================================================================")
        log.info("실행 스레드: {}", currentThread) // 현재 스레드 정보 출력
        log.info("가상 쓰레드 활성화 여부: {}", if (currentThread.isVirtual) "활성화" else "비활성화")
        log.info("애플리케이션 시작! 현재 활성화된 SecurityContextHolder 전략: {}", strategyName)
        log.info("======================================================================")
        return "Current SecurityContextHolderStrategy: $strategyName"
    }
}