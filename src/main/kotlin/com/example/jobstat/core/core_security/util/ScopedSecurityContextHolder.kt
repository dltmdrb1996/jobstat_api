package com.example.jobstat.core.core_security.util

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.stereotype.Component

object ScopedSecurityContextHolder {

    /**
     * SecurityContext를 저장하기 위한 ScopedValue 인스턴스.
     * 이 ScopedValue는 각 가상 스레드 (또는 플랫폼 스레드)의 특정 스코프 내에서
     * SecurityContext를 바인딩하는 데 사용됩니다.
     */
    private val SCOPED_SECURITY_CONTEXT: ScopedValue<SecurityContext> = ScopedValue.newInstance()

    /**
     * 현재 스코프에 바인딩된 SecurityContext를 반환합니다.
     * 바인딩된 컨텍스트가 없으면 새로운 (비어있는) SecurityContext를 생성하여 반환합니다.
     * 이는 기존 SecurityContextHolder.getContext()와 유사하게 동작하도록 하기 위함입니다.
     * (엄밀히 말하면, ScopedValue가 바인딩되지 않았을 때 get()을 호출하면 예외가 발생하므로,
     * isBound 체크가 중요합니다.)
     */
    fun getContext(): SecurityContext {
        return if (SCOPED_SECURITY_CONTEXT.isBound) {
            SCOPED_SECURITY_CONTEXT.get()
        } else {
            // ScopedValue가 바인딩되지 않은 경우, 애플리케이션의 정책에 따라
            // 비어있는 컨텍스트를 반환하거나, 예외를 발생시킬 수 있습니다.
            // 여기서는 비어있는 컨텍스트를 반환하여 기존 동작과 유사하게 만듭니다.
            SecurityContextImpl()
        }
    }

    /**
     * 제공된 SecurityContext를 현재 스코프에 바인딩하고,
     * 주어진 작업(task)을 실행한 후 결과를 반환합니다.
     * 작업 실행 후 스코프는 이전 상태로 복원됩니다.
     *
     * @param context 현재 스코프에 바인딩할 SecurityContext.
     * @param task 실행할 작업. 이 작업은 R 타입의 결과를 반환합니다.
     * @return 작업(task)의 실행 결과.
     * @throws Exception 작업(task) 실행 중 발생할 수 있는 모든 예외.
     */
    fun <R> callWithContext(context: SecurityContext, task: () -> R): R {
        val op = ScopedValue.CallableOp<R, RuntimeException> { task() }   // X = RuntimeException
        return ScopedValue.where(SCOPED_SECURITY_CONTEXT, context)
            .call(op)                                                    // ← 더 이상 추론 오류 없음
    }

    /**
     * 새로운 (비어있는) SecurityContext를 현재 스코프에 바인딩하고,
     * 주어진 작업(task)을 실행한 후 결과를 반환합니다.
     * 작업 실행 후 스코프는 이전 상태로 복원됩니다.
     *
     * @param task 실행할 작업. 이 작업은 R 타입의 결과를 반환합니다.
     * @return 작업(task)의 실행 결과.
     * @throws Exception 작업(task) 실행 중 발생할 수 있는 모든 예외.
     */
    fun <R> callWithNewContext(task: () -> R): R {
        val op = ScopedValue.CallableOp<R, RuntimeException> { task() }
        return ScopedValue.where(SCOPED_SECURITY_CONTEXT, SecurityContextImpl())
            .call(op)
    }
    /**
     * 제공된 SecurityContext를 현재 스코프에 바인딩하고,
     * 결과를 반환하지 않는 주어진 작업(task)을 실행합니다.
     * 작업 실행 후 스코프는 이전 상태로 복원됩니다.
     *
     * @param context 현재 스코프에 바인딩할 SecurityContext.
     * @param task 실행할 작업. 이 작업은 결과를 반환하지 않습니다.
     * @throws Exception 작업(task) 실행 중 발생할 수 있는 모든 예외 (run 메서드가 예외를 전파하는 경우).
     */
    fun runWithContext(context: SecurityContext, task: () -> Unit) {
        // ScopedValue.where(scopedValue, value).run(runnableOp)
        // Kotlin 람다 (() -> Unit)는 ScopedValue.run이 기대하는
        // 함수형 인터페이스 (예: RunnableOp<E extends Throwable> 또는 java.lang.Runnable)로
        // SAM 변환을 통해 호환됩니다.
        ScopedValue.where(SCOPED_SECURITY_CONTEXT, context)
            .run(task) // task는 RunnableOp (또는 Runnable)으로 취급됨
    }

    /**
     * 새로운 (비어있는) SecurityContext를 현재 스코프에 바인딩하고,
     * 결과를 반환하지 않는 주어진 작업(task)을 실행합니다.
     * 작업 실행 후 스코프는 이전 상태로 복원됩니다.
     *
     * @param task 실행할 작업. 이 작업은 결과를 반환하지 않습니다.
     * @throws Exception 작업(task) 실행 중 발생할 수 있는 모든 예외 (run 메서드가 예외를 전파하는 경우).
     */
    fun runWithNewContext(task: () -> Unit) {
        val newContext = SecurityContextImpl()
        ScopedValue.where(SCOPED_SECURITY_CONTEXT, newContext)
            .run(task)
    }
}