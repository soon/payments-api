package com.awesoon.exception

open class AppException(override val message: String, cause: Throwable? = null) : RuntimeException(message, cause)
