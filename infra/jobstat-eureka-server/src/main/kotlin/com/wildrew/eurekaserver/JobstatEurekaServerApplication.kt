package com.wildrew.eurekaserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

@SpringBootApplication
@EnableEurekaServer
class JobstatEurekaServerApplication

fun main(args: Array<String>) {
    runApplication<JobstatEurekaServerApplication>(*args)
}
