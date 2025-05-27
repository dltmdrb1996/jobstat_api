package com.wildrew.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class JobstatApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<JobstatApiGatewayApplication>(*args)
}
