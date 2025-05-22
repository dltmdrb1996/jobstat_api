package com.wildrew.jobstat.core.core_jpa_base.id_generator // 패키지 변경 권장

import com.wildrew.jobstat.core.core_jpa_base.id_generator.sharded.ShardedSnowflake
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@AutoConfiguration // 변경
class CoreIdGeneratorAutoConfiguration { // 클래스 이름 변경 권장
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jobstat.core.id-generator.snowflake.node-id:0}") // 프로퍼티 네임스페이스 변경 권장
    private val nodeId: Long = 0L

    @Value("\${jobstat.core.id-generator.snowflake.shard-count:16}") // 프로퍼티 네임스페이스 변경 권장
    private val shardCount: Int = 16

    @Bean("synchronizedSnowflakeGenerator") // 빈 이름 명확화
    @Primary
    @ConditionalOnMissingBean(name = ["synchronizedSnowflakeGenerator"]) // 이름으로 조건
    // 또는 SnowflakeGenerator 타입의 @Primary 빈이 없을 때
    // @ConditionalOnMissingBean(value = SnowflakeGenerator.class, annotation = Primary.class)
    fun synchronizedSnowflakeGenerator(): SnowflakeGenerator {
        val maxNodeId = SynchronizedSnowflake.MAX_NODE_ID
        require(nodeId in 0..maxNodeId) {
            "Configured Snowflake Node ID ($nodeId) is out of range (0-$maxNodeId). " +
                    "Check jobstat.core.id-generator.snowflake.node-id property."
        }
        if (nodeId == 0L) {
            log.warn(
                "Using default Snowflake Node ID 0 for synchronizedSnowflakeGenerator. " +
                        "Ensure this is intended for multi-instance deployments."
            )
        } else {
            log.debug("Creating SynchronizedSnowflakeGenerator Bean with Node ID: {}", nodeId)
        }
        return SynchronizedSnowflake(nodeId)
    }

    @Bean("shardedSnowflakeGenerator") // 빈 이름 명확화
    @ConditionalOnMissingBean(name = ["shardedSnowflakeGenerator"]) // 이름으로 조건
    fun shardedSnowflakeGenerator(): SnowflakeGenerator {
        val maxNodeId = ShardedSnowflake.MAX_NODE_ID
        require(nodeId in 0..maxNodeId) {
            "Configured Snowflake Node ID ($nodeId) for shardedSnowflakeGenerator is out of range (0-$maxNodeId). " +
                    "Check jobstat.core.id-generator.snowflake.node-id property."
        }
        log.debug("Creating ShardedSnowflakeGenerator Bean with Node ID: {}, Shard Count: {}", nodeId, shardCount)
        return ShardedSnowflake(nodeId, shardCount)
    }

//    @Bean("hibernateSnowflakeIdGenerator") // 빈 이름 명확화
//    @ConditionalOnMissingBean(HibernateSnowflakeIdGenerator::class)
//    @ConditionalOnClass(IdentifierGenerator::class) // Hibernate의 IdentifierGenerator 존재 시
//    fun hibernateSnowflakeIdGenerator(
//        // 기본적으로 @Primary로 지정된 synchronizedSnowflakeGenerator를 주입받음
//        // 다른 것을 사용하고 싶다면 사용자가 @Primary 빈을 직접 등록하거나,
//        // 이 라이브러리에서 프로퍼티로 어떤 SnowflakeGenerator를 사용할지 선택하도록 할 수 있음
//        @Qualifier("synchronizedSnowflakeGenerator") snowflakeGenerator: SnowflakeGenerator
//    ): HibernateSnowflakeIdGenerator {
//        log.debug("Creating HibernateSnowflakeIdGenerator Bean using {}", snowflakeGenerator.javaClass.simpleName)
//        return HibernateSnowflakeIdGenerator(snowflakeGenerator)
//    }
}