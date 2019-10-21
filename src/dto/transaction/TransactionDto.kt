package com.awesoon.dto.transaction

import com.awesoon.domain.Transaction

data class TransactionDto(val id: Long, val status: String) {
    object ModelMapper {
        fun from(obj: Transaction) = TransactionDto(obj.id.value, obj.status)
    }
}
