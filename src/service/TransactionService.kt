package com.awesoon.service

import com.awesoon.domain.Transaction
import java.math.BigDecimal

interface TransactionService {
    suspend fun createTransaction(fromId: Long, toId: Long, amount: BigDecimal): Transaction
    suspend fun executeTransaction(txId: Long)
    suspend fun countPendingTransactions(): Int
    suspend fun getTransactionById(id: Long): Transaction?
}
