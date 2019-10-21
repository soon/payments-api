package com.awesoon.exception

class ValidationException(
    message: String = "Validation failed",
    val fieldErrors: Map<String, Any>? = null,
    val nonFieldErrors: List<String>? = null
) : AppException(message)
