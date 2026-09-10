package com.borrowingservice.web

import java.time.ZonedDateTime

data class CommandResponse(val id: String)

data class ApiError(
    val status: Int,
    val message: String,
    val timestamp: ZonedDateTime = ZonedDateTime.now()
)
