package com.wildrew.jobstat.core.core_jpa_base.id_generator

import com.wildrew.jobstat.core.core_jpa_base.id_generator.sharded.ShardedSnowflake
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@AutoConfiguration
class CoreIdGeneratorAutoConfiguration {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jobstat.core.id-generator.snowflake.node-id:0}")
    private val nodeId: Long = 0L

    @Value("\${jobstat.core.id-generator.snowflake.shard-count:16}")
    private val shardCount: Int = 16

    @Bean("synchronizedSnowflakeGenerator")
    @Primary
    @ConditionalOnMissingBean(name = ["synchronizedSnowflakeGenerator"])
    fun synchronizedSnowflakeGenerator(): SnowflakeGenerator {
        val maxNodeId = SynchronizedSnowflake.MAX_NODE_ID
        require(nodeId in 0..maxNodeId) {
            "Configured Snowflake Node ID ($nodeId) is out of range (0-$maxNodeId). " +
                "Check jobstat.core.id-generator.snowflake.node-id property."
        }
        if (nodeId == 0L) {
            log.warn(
                "Using default Snowflake Node ID 0 for synchronizedSnowflakeGenerator. " +
                    "Ensure this is intended for multi-instance deployments.",
            )
        } else {
            log.debug("Creating SynchronizedSnowflakeGenerator Bean with Node ID: {}", nodeId)
        }
        return SynchronizedSnowflake(nodeId)
    }

    @Bean("shardedSnowflakeGenerator")
    @ConditionalOnMissingBean(name = ["shardedSnowflakeGenerator"])
    fun shardedSnowflakeGenerator(): SnowflakeGenerator {
        val maxNodeId = ShardedSnowflake.MAX_NODE_ID
        require(nodeId in 0..maxNodeId) {
            "Configured Snowflake Node ID ($nodeId) for shardedSnowflakeGenerator is out of range (0-$maxNodeId). " +
                "Check jobstat.core.id-generator.snowflake.node-id property."
        }
        log.debug("Creating ShardedSnowflakeGenerator Bean with Node ID: {}, Shard Count: {}", nodeId, shardCount)
        return ShardedSnowflake(nodeId, shardCount)
    }
}
