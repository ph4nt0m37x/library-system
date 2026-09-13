package com.mcpservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class McpServiceApplication

fun main(args: Array<String>) {
    runApplication<McpServiceApplication>(*args)
}
