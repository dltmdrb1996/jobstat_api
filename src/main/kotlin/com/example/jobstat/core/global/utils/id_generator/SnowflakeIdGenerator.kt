package com.example.jobstat.core.global.utils.id_generator

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.IdentifierGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component // Spring Bean으로 관리될 경우 필요
import java.io.Serializable

@Component // 필요시 Bean으로 등록 (생성자 주입 방식 권장)
class SnowflakeIdGenerator(
    private val snowFlakeGenerator: SnowflakeGenerator,
) : IdentifierGenerator {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun generate(
        session: SharedSessionContractImplementor?,
        obj: Any?,
    ): Serializable {
        val id = snowFlakeGenerator.nextId()
        log.debug("호출된다 id $id")
        return id
    }
}
