package com.awesoon.dto.exception

import com.awesoon.exception.ValidationException

data class ValidationExceptionDto(
    val message: String,
    val fieldErrors: Map<String, Any>? = null,
    val nonFieldErrors: List<String>? = null
) {
    object ModelMapper {
        fun from(ex: ValidationException) = ValidationExceptionDto(ex.message, ex.fieldErrors, ex.nonFieldErrors)
    }
}
