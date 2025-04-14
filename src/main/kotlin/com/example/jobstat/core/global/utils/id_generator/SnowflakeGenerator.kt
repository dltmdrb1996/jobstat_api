package com.example.jobstat.core.global.utils.id_generator

/**
 * Snowflake ID 생성기의 공통 인터페이스
 */
interface SnowflakeGenerator {
    /**
     * 다음 고유 ID를 생성한다.
     * @return 생성된 64비트 Long ID
     * @throws IllegalStateException 시계 역행 등 ID 생성 불가 시 발생 가능
     */
    @Throws(IllegalStateException::class)
    fun nextId(): Long
}
