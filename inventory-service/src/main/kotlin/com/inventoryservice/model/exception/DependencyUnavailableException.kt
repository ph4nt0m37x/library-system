package com.inventoryservice.model.exception

class DependencyUnavailableException(
    val dependency: String,
    cause: Throwable? = null
) : RuntimeException("$dependency service is unavailable", cause)
