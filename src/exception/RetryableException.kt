package com.awesoon.exception

class RetryableException(message: String = "retry", cause: Throwable? = null) : AppException(message, cause)
