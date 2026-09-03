package com.borrowingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BorrowingServiceApplication

fun main(args: Array<String>) {
    runApplication<BorrowingServiceApplication>(*args)
}
