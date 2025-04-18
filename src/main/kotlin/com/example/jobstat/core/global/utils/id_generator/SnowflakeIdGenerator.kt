package com.example.jobstat.core.global.utils.id_generator

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.IdentifierGenerator
import org.springframework.stereotype.Component // Spring Bean으로 관리될 경우 필요
import java.io.Serializable

@Component
class SnowflakeIdGenerator(
    private val snowFlakeGenerator: SnowflakeGenerator,
) : IdentifierGenerator {
    override fun generate(
        session: SharedSessionContractImplementor?,
        obj: Any?,
    ): Serializable {
        val id = snowFlakeGenerator.nextId()
        return id
    }
}
