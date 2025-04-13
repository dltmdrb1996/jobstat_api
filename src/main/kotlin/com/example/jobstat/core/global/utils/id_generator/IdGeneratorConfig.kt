package com.example.jobstat.core.global.utils.id_generator

import com.example.jobstat.core.global.utils.id_generator.sharded.ShardedSnowflake // 실제 경로 확인
import com.example.jobstat.core.global.utils.id_generator.SynchronizedSnowflake // 실제 경로 확인
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class IdGeneratorConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Snowflake Node ID 설정.
     * 우선순위:
     * 1. 시스템 프로퍼티 (-Dsnowflake.node-id=X)
     * 2. 환경 변수 (SNOWFLAKE_NODE_ID=X)
     * 3. application.yml 또는 application.properties 파일의 'snowflake.node-id'
     * 4. 아래 지정된 기본값 (0L)
     *
     * 향후 오토스케일링 환경 등에서 환경 변수 주입 시 자동으로 해당 값을 사용하게 됩니다.
     * 현재 수동으로 여러 인스턴스를 실행하는 경우, 각 인스턴스에 고유한 ID를
     * 환경 변수나 시스템 프로퍼티로 설정해 주어야 합니다. (예: export SNOWFLAKE_NODE_ID=1)
     */
    @Value("\${snowflake.node-id:0}") // 기본값을 0으로 설정
    private val nodeId: Long = 0L // 초기값일 뿐, @Value에 의해 덮어써짐

    @Value("\${snowflake.shard-count:16}")
    private val shardCount: Int = 16

    @Bean("synchronizedSnowflake")
    @Primary
    fun synchronizedSnowflakeGenerator(): SnowflakeGenerator {
        // 설정된 nodeId 값 검증 (중요)
        val maxNodeId = SynchronizedSnowflake.MAX_NODE_ID // 실제 MAX_NODE_ID 상수 접근
        require(nodeId in 0..maxNodeId) {
            "Configured Snowflake Node ID ($nodeId) is out of range (0-$maxNodeId). " +
                    "Please check environment variable 'SNOWFLAKE_NODE_ID', system property 'snowflake.node-id', or application config."
        }

        // 기본값(0)이 사용될 경우 경고 로그 출력 (운영 환경 주의)
        if (nodeId == 0L) {
            log.warn("Using default Snowflake Node ID 0. Ensure this is intended, especially in multi-instance deployments. " +
                    "Consider setting a unique SNOWFLAKE_NODE_ID environment variable or -Dsnowflake.node-id system property for each instance.")
        } else {
            log.info("Creating SynchronizedSnowflakeGenerator Bean with Node ID: {}", nodeId)
        }
        return SynchronizedSnowflake(nodeId)
    }

    @Bean("shardedSnowflake")
    fun shardedSnowflakeGenerator(): SnowflakeGenerator {
        // 설정된 nodeId 값 검증
        val maxNodeId = ShardedSnowflake.MAX_NODE_ID // ShardedSnowflake의 MAX_NODE_ID 사용 (가정)
        require(nodeId in 0..maxNodeId) { // ShardedSnowflake 구현에 따라 maxNodeId가 다를 수 있음
            "Configured Snowflake Node ID ($nodeId) for sharded generator is out of range (0-$maxNodeId)."
        }
        log.info("Creating ShardedSnowflake Bean with Node ID: {}, Shard Count: {}", nodeId, shardCount)
        // ShardedSnowflake 구현 시 nodeId 범위 검증 필요
        return ShardedSnowflake(nodeId, shardCount) // 실제 ShardedSnowflake 생성자 사용
    }
}