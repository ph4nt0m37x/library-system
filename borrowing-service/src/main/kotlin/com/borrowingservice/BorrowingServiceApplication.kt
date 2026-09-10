package com.borrowingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class BorrowingServiceApplication

fun main(args: Array<String>) {
    runApplication<BorrowingServiceApplication>(*args)
}
