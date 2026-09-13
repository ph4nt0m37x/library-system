package com.inventoryservice.web

import com.inventoryservice.model.exception.InvalidStockQuantityException
import com.inventoryservice.model.exception.DomainConflictException
import com.inventoryservice.model.exception.DomainValidationException
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.model.exception.DependencyUnavailableException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MethodArgumentNotValidException
import java.util.concurrent.CompletionException

@RestControllerAdvice
class InventoryCommandExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBeanValidation(exception: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val detail = exception.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage ?: "invalid value"}" }

        return problem(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            detail.ifBlank { "Request validation failed" }
        )
    }

    @ExceptionHandler(CompletionException::class)
    fun handleCommandFailure(exception: CompletionException): ResponseEntity<ProblemDetail> {
        val invalidQuantity = findInvalidQuantityCause(exception)
        val validation = findCause<DomainValidationException>(exception)
        val notFound = findCause<ResourceNotFoundException>(exception)
        val conflict = findCause<DomainConflictException>(exception)
        val illegalArgument = findCause<IllegalArgumentException>(exception)
        val illegalState = findCause<IllegalStateException>(exception)

        return when {
            invalidQuantity != null ->
                invalidQuantity(invalidQuantity.message ?: "Invalid stock quantity")
            validation != null ->
                problem(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", validation.message)
            notFound != null ->
                problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", notFound.message)
            conflict != null ->
                conflict(conflict.message)
            illegalState != null ->
                conflict(illegalState.message)
            illegalArgument != null ->
                problem(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", illegalArgument.message)
            else ->
                problem(HttpStatus.INTERNAL_SERVER_ERROR, "COMMAND_FAILED", "The command could not be completed.")
        }
    }

    @ExceptionHandler(InvalidStockQuantityException::class)
    fun handleInvalidQuantity(exception: InvalidStockQuantityException): ResponseEntity<ProblemDetail> =
        invalidQuantity(exception.message ?: "Invalid stock quantity")

    @ExceptionHandler(DomainValidationException::class)
    fun handleValidation(exception: DomainValidationException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", exception.message)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(exception: ResourceNotFoundException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.message)

    @ExceptionHandler(DomainConflictException::class)
    fun handleDomainConflict(exception: DomainConflictException): ResponseEntity<ProblemDetail> =
        conflict(exception.message)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidArgument(exception: IllegalArgumentException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", exception.message)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(exception: IllegalStateException): ResponseEntity<ProblemDetail> =
        conflict(exception.message)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConstraintViolation(exception: DataIntegrityViolationException): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.CONFLICT,
            "CONSTRAINT_VIOLATION",
            "The requested data conflicts with an existing resource"
        )

    @ExceptionHandler(DependencyUnavailableException::class)
    fun handleDependencyUnavailable(exception: DependencyUnavailableException): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.SERVICE_UNAVAILABLE,
            "${exception.dependency.uppercase()}_UNAVAILABLE",
            exception.message
        )

    private fun findInvalidQuantityCause(exception: Throwable): InvalidStockQuantityException? {
        return findCause(exception)
    }

    private inline fun <reified T : Throwable> findCause(exception: Throwable): T? {
        var current: Throwable? = exception

        while (current != null) {
            if (current is T) {
                return current
            }
            current = current.cause
        }

        return null
    }

    private fun conflict(detail: String?): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.CONFLICT,
            "INVENTORY_CONFLICT",
            detail ?: "The inventory operation cannot be completed"
        )

    private fun invalidQuantity(detail: String): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "INVALID_STOCK_QUANTITY", detail)

    private fun problem(status: HttpStatus, code: String, detail: String?): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(
            status,
            detail ?: "The inventory request could not be completed"
        )
        problem.setProperty("code", code)
        return ResponseEntity.status(status).body(problem)
    }
}
