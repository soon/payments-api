package com.awesoon.dto.exception

import com.awesoon.exception.AppException

data class AppExceptionDto(val message: String) {
    object ModelMapper {
        fun from(ex: AppException) = AppExceptionDto(ex.message)
    }
}
