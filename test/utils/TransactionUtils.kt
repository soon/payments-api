package com.awesoon.utils

import com.awesoon.dto.account.AccountDto
import com.awesoon.dto.stats.transaction.PendingTransactionsCountDto
import com.awesoon.dto.transaction.NewTransactionDto
import com.awesoon.dto.transaction.TransactionDto
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import kotlinx.coroutines.delay
import java.math.BigDecimal

fun TestApplicationEngine.makeTransaction(dto: NewTransactionDto): TransactionDto =
    postJson("/api/money/transfer", dto).fromJson()

fun TestApplicationEngine.makeTransaction(from: AccountDto, to: AccountDto, amount: BigDecimal) =
    makeTransaction(NewTransactionDto(fromId = from.id, toId = to.id, amount = amount))

fun TestApplicationEngine.getTransactionById(id: Long): TransactionDto =
    handleRequest(HttpMethod.Get, "/api/internal/transactions/$id").fromJson()

fun TestApplicationEngine.getPendingTransactions(): PendingTransactionsCountDto =
    handleRequest(HttpMethod.Get, "/api/internal/transactions/pending").fromJson()

suspend fun TestApplicationEngine.awaitTransactions() {
    var pendingTransactions: PendingTransactionsCountDto = getPendingTransactions()
    while (pendingTransactions.count != 0) {
        delay(1000)
        pendingTransactions = getPendingTransactions()
    }
}
