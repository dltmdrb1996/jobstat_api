package com.example.jobstat.core.global.utils

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
