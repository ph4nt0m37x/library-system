package com.membershipservice.web

import org.axonframework.commandhandling.CommandExecutionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.ZonedDateTime
import java.util.concurrent.CompletionException

data class ApiError(
    val status: Int,
    val message: String,
    val timestamp: ZonedDateTime = ZonedDateTime.now()
)

@RestControllerAdvice
class RestExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun invalidRequest(exception: IllegalArgumentException): ResponseEntity<ApiError> =
        error(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid request")

    @ExceptionHandler(CommandExecutionException::class, CompletionException::class)
    fun commandFailed(exception: RuntimeException): ResponseEntity<ApiError> {
        val cause = generateSequence(exception as Throwable?) { it.cause }.last()
        val status = if (cause is IllegalArgumentException) HttpStatus.BAD_REQUEST else HttpStatus.CONFLICT
        return error(status, cause.message ?: "Command could not be completed")
    }

    private fun error(status: HttpStatus, message: String) =
        ResponseEntity.status(status).body(ApiError(status.value(), message))
}
