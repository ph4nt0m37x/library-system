package com.borrowingservice.web

import com.borrowingservice.client.MembershipMemberNotFoundException
import com.borrowingservice.client.MembershipServiceUnavailableException
import com.borrowingservice.model.valueObject.LoanEligibilityException
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.model.valueObject.StateConflictException
import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.modelling.command.AggregateNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestBodyException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.concurrent.CompletionException

@RestControllerAdvice
class RestExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun resourceNotFound(exception: ResourceNotFoundException): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.NOT_FOUND,
            "${exception.resource.uppercase().replace(' ', '_')}_NOT_FOUND",
            exception.message ?: "Requested resource was not found"
        )

    @ExceptionHandler(AggregateNotFoundException::class)
    fun aggregateNotFound(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Requested resource was not found")

    @ExceptionHandler(MembershipMemberNotFoundException::class)
    fun membershipMemberNotFound(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member was not found")

    @ExceptionHandler(MembershipServiceUnavailableException::class)
    fun membershipUnavailable(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.SERVICE_UNAVAILABLE, "MEMBERSHIP_UNAVAILABLE", "Membership service is unavailable")

    @ExceptionHandler(StateConflictException::class)
    fun stateConflict(exception: StateConflictException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, exception.code, exception.message)

    @ExceptionHandler(LoanEligibilityException::class)
    fun loanEligibilityConflict(exception: LoanEligibilityException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, exception.reason.name, exception.message ?: "Loan is not eligible")

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun constraintConflict(): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.CONFLICT,
            "CONSTRAINT_VIOLATION",
            "The requested data conflicts with an existing resource"
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun invalidRequest(exception: IllegalArgumentException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.message ?: "Invalid request")

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MethodArgumentNotValidException::class,
        MissingServletRequestBodyException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun malformedRequest(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request")

    @ExceptionHandler(CommandExecutionException::class, CompletionException::class)
    fun commandFailed(exception: RuntimeException): ResponseEntity<ProblemDetail> = classify(exception)

    @ExceptionHandler(Exception::class)
    fun unexpectedFailure(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Request could not be completed")

    private fun classify(exception: Throwable): ResponseEntity<ProblemDetail> {
        causeOf<ResourceNotFoundException>(exception)?.let {
            return resourceNotFound(it)
        }
        if (causeOf<AggregateNotFoundException>(exception) != null) {
            return aggregateNotFound()
        }
        if (causeOf<MembershipMemberNotFoundException>(exception) != null) {
            return membershipMemberNotFound()
        }
        if (causeOf<MembershipServiceUnavailableException>(exception) != null) {
            return membershipUnavailable()
        }
        causeOf<StateConflictException>(exception)?.let {
            return stateConflict(it)
        }
        causeOf<LoanEligibilityException>(exception)?.let {
            return loanEligibilityConflict(it)
        }
        if (causeOf<DataIntegrityViolationException>(exception) != null) {
            return constraintConflict()
        }
        causeOf<IllegalArgumentException>(exception)?.let {
            return invalidRequest(it)
        }
        return unexpectedFailure()
    }

    private inline fun <reified T : Throwable> causeOf(exception: Throwable): T? {
        var current: Throwable? = exception
        while (current != null) {
            if (current is T) {
                return current
            }
            current = current.cause
        }
        return null
    }

    private fun problem(status: HttpStatus, code: String, detail: String): ResponseEntity<ProblemDetail> {
        val body = ProblemDetail.forStatus(status)
        body.title = status.reasonPhrase
        body.detail = detail
        body.setProperty("code", code)
        return ResponseEntity.status(status).body(body)
    }
}
