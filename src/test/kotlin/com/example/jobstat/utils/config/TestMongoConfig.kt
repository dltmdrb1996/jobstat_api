package com.example.jobstat.utils.config

import org.springframework.boot.test.context.TestConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@TestConfiguration
@Testcontainers
class TestMongoConfig {
    companion object {
        const val MONGO_VERSION = "5.0.15"
        const val MONGO_DATABASE = "test_database"
        const val MONGO_USERNAME = "test"
        const val MONGO_PASSWORD = "test"

        @Container
        val mongoContainer =
            GenericContainer(
                DockerImageName.parse("mongo:$MONGO_VERSION"),
            ).withExposedPorts(27017)
                .withReuse(true)
                .withEnv(
                    mapOf(
                        "MONGO_INITDB_ROOT_USERNAME" to MONGO_USERNAME,
                        "MONGO_INITDB_ROOT_PASSWORD" to MONGO_PASSWORD,
                        "MONGO_INITDB_DATABASE" to MONGO_DATABASE,
                    ),
                )

        init {
            mongoContainer.start()
        }
    }
}
