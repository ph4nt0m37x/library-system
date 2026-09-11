package com.catalogservice.web

import com.catalogservice.model.exception.BookAlreadyDeletedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.axonframework.modelling.command.AggregateNotFoundException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.concurrent.CompletionException

@RestControllerAdvice
class CatalogCommandExceptionHandler {

    @ExceptionHandler(CompletionException::class)
    fun handleCommandFailure(exception: CompletionException): ResponseEntity<ProblemDetail> {
        val alreadyDeleted = findCause(exception, BookAlreadyDeletedException::class.java)
        val missingAggregate = findCause(exception, AggregateNotFoundException::class.java)

        return when {
            alreadyDeleted != null ->
                problem(HttpStatus.CONFLICT, "BOOK_ALREADY_DELETED", alreadyDeleted.message!!)
            missingAggregate != null ->
                problem(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book was not found.")
            else ->
                problem(HttpStatus.INTERNAL_SERVER_ERROR, "COMMAND_FAILED", "The command could not be completed.")
        }
    }

    @ExceptionHandler(BookAlreadyDeletedException::class)
    fun handleAlreadyDeleted(exception: BookAlreadyDeletedException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "BOOK_ALREADY_DELETED", exception.message!!)

    @ExceptionHandler(AggregateNotFoundException::class)
    fun handleMissingAggregate(exception: AggregateNotFoundException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book was not found.")

    private fun <T : Throwable> findCause(exception: Throwable, type: Class<T>): T? {
        var current: Throwable? = exception

        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current)
            }
            current = current.cause
        }

        return null
    }

    private fun problem(status: HttpStatus, code: String, detail: String): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.setProperty("code", code)
        return ResponseEntity.status(status).body(problem)
    }
}
