package com.example.jobstat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableJpaAuditing
@EnableConfigurationProperties(MongoProperties::class)
class JobstatApplication

fun main(args: Array<String>) {
    runApplication<JobstatApplication>(*args)
}
