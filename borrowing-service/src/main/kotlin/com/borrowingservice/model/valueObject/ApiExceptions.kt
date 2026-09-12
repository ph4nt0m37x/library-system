package com.borrowingservice.model.valueObject

class ResourceNotFoundException(
    val resource: String,
    val resourceId: String
) : RuntimeException("$resource $resourceId was not found")

class StateConflictException(
    val code: String,
    override val message: String
) : RuntimeException(message)
