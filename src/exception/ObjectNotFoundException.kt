package com.awesoon.exception

class ObjectNotFoundException(message: String) : AppException(message) {
    companion object {
        inline fun <reified T : Any> ofType() = ObjectNotFoundException("${T::class.simpleName} not found")
    }
}
