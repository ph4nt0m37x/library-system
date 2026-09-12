package com.inventoryservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.cloud.openfeign.EnableFeignClients
import java.time.Clock

@SpringBootApplication
@EnableFeignClients
class InventoryServiceApplication {
    @Bean
    fun inventoryClock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}
