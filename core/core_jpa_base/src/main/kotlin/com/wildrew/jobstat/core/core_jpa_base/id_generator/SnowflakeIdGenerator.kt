package com.wildrew.jobstat.core.core_jpa_base.id_generator

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.IdentifierGenerator
import java.io.Serializable

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
