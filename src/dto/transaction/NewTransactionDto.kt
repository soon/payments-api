package com.awesoon.dto.transaction

import java.math.BigDecimal

data class NewTransactionDto(val fromId: Long, val toId: Long, val amount: BigDecimal)
