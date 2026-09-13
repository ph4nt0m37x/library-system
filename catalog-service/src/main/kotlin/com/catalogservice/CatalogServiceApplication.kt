package com.catalogservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock

@SpringBootApplication
@EnableScheduling
class CatalogServiceApplication {
    @Bean
    fun catalogClock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<CatalogServiceApplication>(*args)
}
