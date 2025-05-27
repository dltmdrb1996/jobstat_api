package com.wildrew.jobstat.auth.utils.config

import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
class DockerTestConfig {
    init {
        checkDockerAvailability()
    }

    private fun checkDockerAvailability() {
        try {
            val process = Runtime.getRuntime().exec("docker ps")
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IllegalStateException("Docker is not running. Please start Docker and try again.")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to check Docker status: ${e.message}")
        }
    }
}
