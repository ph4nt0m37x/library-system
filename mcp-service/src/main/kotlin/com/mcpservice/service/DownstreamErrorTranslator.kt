package com.mcpservice.service

import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import feign.FeignException
import feign.RetryableException
import feign.codec.DecodeException
import org.springframework.stereotype.Component

@Component
class DownstreamErrorTranslator {
    fun <T> call(
        dependency: String,
        notFoundCode: ToolErrorCode,
        operation: () -> T
    ): T = try {
        operation()
    } catch (exception: LibraryMcpToolException) {
        throw exception
    } catch (exception: DecodeException) {
        throw LibraryMcpToolException(
            ToolErrorCode.DOWNSTREAM_CONTRACT_ERROR,
            "$dependency returned an unexpected response."
        )
    } catch (exception: RetryableException) {
        throw unavailable(dependency)
    } catch (exception: FeignException) {
        throw translateHttp(dependency, notFoundCode, exception)
    } catch (_: RuntimeException) {
        throw LibraryMcpToolException(
            ToolErrorCode.INTERNAL_ERROR,
            "The request could not be completed."
        )
    }

    fun <T> optionalOnNotFound(
        dependency: String,
        operation: () -> T
    ): T? = try {
        operation()
    } catch (exception: DecodeException) {
        throw LibraryMcpToolException(
            ToolErrorCode.DOWNSTREAM_CONTRACT_ERROR,
            "$dependency returned an unexpected response."
        )
    } catch (exception: RetryableException) {
        throw unavailable(dependency)
    } catch (exception: FeignException) {
        if (exception.status() == 404) null
        else throw translateHttp(dependency, ToolErrorCode.INTERNAL_ERROR, exception)
    }

    private fun translateHttp(
        dependency: String,
        notFoundCode: ToolErrorCode,
        exception: FeignException
    ): LibraryMcpToolException {
        val downstreamCode = extractJsonString(exception.contentUTF8(), "code")
        val mappedCode = knownCode(downstreamCode) ?: when (exception.status()) {
            400, 422 -> ToolErrorCode.INVALID_ARGUMENT
            404 -> notFoundCode
            409 -> conflictFor(notFoundCode)
            in 500..599, -1 -> ToolErrorCode.DEPENDENCY_UNAVAILABLE
            else -> ToolErrorCode.INTERNAL_ERROR
        }
        val message = when (mappedCode) {
            ToolErrorCode.BOOK_NOT_FOUND -> "The requested book was not found."
            ToolErrorCode.LIBRARY_NOT_FOUND -> "The requested library was not found."
            ToolErrorCode.MEMBER_NOT_FOUND -> "The requested member was not found."
            ToolErrorCode.LOAN_NOT_FOUND -> "The requested loan was not found."
            ToolErrorCode.FEE_NOT_FOUND -> "The requested fee was not found."
            ToolErrorCode.MEMBERSHIP_INACTIVE -> "The member does not have an active membership."
            ToolErrorCode.NO_STOCK -> "No borrowable copy is available at the selected library."
            ToolErrorCode.LOAN_CONFLICT -> "The loan action conflicts with the current circulation state."
            ToolErrorCode.PAYMENT_CONFLICT -> "The payment conflicts with existing payment or fee data."
            ToolErrorCode.INVALID_ARGUMENT -> "The downstream service rejected one or more arguments."
            ToolErrorCode.DEPENDENCY_UNAVAILABLE -> "$dependency is currently unavailable."
            else -> "The request could not be completed."
        }
        return LibraryMcpToolException(mappedCode, message)
    }

    private fun knownCode(code: String?): ToolErrorCode? = when (code) {
        "BOOK_NOT_FOUND", "CATALOG_BOOK_NOT_FOUND" -> ToolErrorCode.BOOK_NOT_FOUND
        "LIBRARY_NOT_FOUND" -> ToolErrorCode.LIBRARY_NOT_FOUND
        "MEMBER_NOT_FOUND" -> ToolErrorCode.MEMBER_NOT_FOUND
        "LOAN_NOT_FOUND" -> ToolErrorCode.LOAN_NOT_FOUND
        "FEE_NOT_FOUND" -> ToolErrorCode.FEE_NOT_FOUND
        "MEMBERSHIP_INACTIVE" -> ToolErrorCode.MEMBERSHIP_INACTIVE
        "NO_STOCK", "INSUFFICIENT_STOCK" -> ToolErrorCode.NO_STOCK
        "PAYMENT_CONFLICT" -> ToolErrorCode.PAYMENT_CONFLICT
        "ACTIVE_LOAN_LIMIT_REACHED", "TOO_MANY_UNPAID_FEES", "MEMBER_TEMPORARILY_BANNED",
        "MEMBER_PERMANENTLY_BANNED", "LOAN_CONFLICT" -> ToolErrorCode.LOAN_CONFLICT
        "INVALID_ARGUMENT", "INVALID_REQUEST", "VALIDATION_ERROR", "MALFORMED_REQUEST" ->
            ToolErrorCode.INVALID_ARGUMENT
        "MEMBERSHIP_UNAVAILABLE", "INVENTORY_UNAVAILABLE", "CATALOG_UNAVAILABLE" ->
            ToolErrorCode.DEPENDENCY_UNAVAILABLE
        else -> null
    }

    private fun conflictFor(notFoundCode: ToolErrorCode): ToolErrorCode =
        if (notFoundCode == ToolErrorCode.FEE_NOT_FOUND) ToolErrorCode.PAYMENT_CONFLICT
        else ToolErrorCode.LOAN_CONFLICT

    private fun unavailable(dependency: String) = LibraryMcpToolException(
        ToolErrorCode.DEPENDENCY_UNAVAILABLE,
        "$dependency is currently unavailable."
    )

    private fun extractJsonString(json: String, field: String): String? {
        if (json.isBlank()) return null
        val pattern = Regex("\\\"${Regex.escape(field)}\\\"\\s*:\\s*\\\"([^\\\"]{1,100})\\\"")
        return pattern.find(json)?.groupValues?.get(1)
    }
}
