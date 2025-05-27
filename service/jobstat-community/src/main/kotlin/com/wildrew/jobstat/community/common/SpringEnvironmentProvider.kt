package com.wildrew.jobstat.community.common

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

interface EnvironmentProvider {
    fun isDevelopmentEnvironment(): Boolean
}

@Component
class EnvironmentProviderImpl(
    private val environment: Environment,
) : EnvironmentProvider {
    override fun isDevelopmentEnvironment(): Boolean = environment.activeProfiles.contains("dev")
}
