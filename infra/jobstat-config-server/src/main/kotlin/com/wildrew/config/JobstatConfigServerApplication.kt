package com.wildrew.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer

@SpringBootApplication
@EnableConfigServer
class JobstatConfigServerApplication

fun main(args: Array<String>) {
    runApplication<JobstatConfigServerApplication>(*args)
}