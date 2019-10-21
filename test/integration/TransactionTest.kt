package com.awesoon.integration

import com.awesoon.domain.TransactionStatus
import com.awesoon.dto.exception.AppExceptionDto
import com.awesoon.dto.transaction.NewTransactionDto
import com.awesoon.utils.assertContentJsonIs
import com.awesoon.utils.assertStatusIs
import com.awesoon.utils.awaitTransactions
import com.awesoon.utils.createAccount
import com.awesoon.utils.getAccountById
import com.awesoon.utils.getTransactionById
import com.awesoon.utils.makeTransaction
import com.awesoon.utils.postJson
import com.awesoon.utils.withTestApiApplication
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionTest {
    @Test
    fun `test money transferring`() = withTestApiApplication {
        val acc1 = createAccount(BigDecimal.ONE)
        val acc2 = createAccount(BigDecimal.ZERO)

        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = acc1.id, toId = acc2.id, amount = BigDecimal.ONE
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.OK)
        }
    }

    @Test
    fun `test money transferring with completed transactions`() = withTestApiApplication {
        val acc1 = createAccount(BigDecimal.ONE)
        val acc2 = createAccount(BigDecimal.ZERO)
        makeTransaction(acc1, acc2, BigDecimal.ONE)

        runBlocking {
            withTimeout(10_000) {
                awaitTransactions()
            }
        }

        with(getAccountById(acc1.id)) {
            assertEquals(BigDecimal.ZERO, balance)
        }
        with(getAccountById(acc2.id)) {
            assertEquals(BigDecimal.ONE, balance)
        }
    }

    @Test
    fun `test money transferring with invalid accounts`() = withTestApiApplication {
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = 1, toId = 2, amount = BigDecimal.ONE
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "invalid fromId"
                )
            )
        }
    }

    @Test
    fun `test money transferring with invalid sender`() = withTestApiApplication {
        val account = createAccount(BigDecimal.ONE)
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = -1, toId = account.id, amount = BigDecimal.ONE
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "invalid fromId"
                )
            )
        }
    }

    @Test
    fun `test money transferring with invalid receiver`() = withTestApiApplication {
        val account = createAccount(BigDecimal.ONE)
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = account.id, toId = -1, amount = BigDecimal.ONE
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "invalid toId"
                )
            )
        }
    }

    @Test
    fun `test not enough money to transfer`() = withTestApiApplication {
        val sender = createAccount(BigDecimal.ONE)
        val receiver = createAccount(BigDecimal.ONE)
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = sender.id, toId = receiver.id, amount = BigDecimal.TEN
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "not enough money"
                )
            )
        }
    }

    @Test
    fun `test max amount scale`() = withTestApiApplication {
        val sender = createAccount(BigDecimal("1234"))
        val receiver = createAccount(BigDecimal.ONE)
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = sender.id, toId = receiver.id, amount = BigDecimal("1230.0000000001")
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.OK)
        }
    }

    @Test
    fun `test invalid amount scale`() = withTestApiApplication {
        val sender = createAccount(BigDecimal.ONE)
        val receiver = createAccount(BigDecimal.ONE)
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = sender.id, toId = receiver.id, amount = BigDecimal("0.00000000001")
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "invalid amount"
                )
            )
        }
    }

    @Test
    fun `test max amount precision`() = withTestApiApplication {
        val amount = BigDecimal.TEN.pow(20) - BigDecimal("0.0000000001")
        val sender = createAccount(amount)
        val receiver = createAccount(BigDecimal.ONE)
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = sender.id, toId = receiver.id, amount = amount
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.OK)
        }
    }

    @Test
    fun `test invalid amount precision`() = withTestApiApplication {
        val sender = createAccount(BigDecimal.ONE)
        val receiver = createAccount(BigDecimal.ONE)
        val call = postJson(
            "/api/money/transfer", NewTransactionDto(
                fromId = sender.id, toId = receiver.id, amount = BigDecimal.TEN.pow(20)
            )
        )
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "invalid amount"
                )
            )
        }
    }

    @Test
    fun `test invalid final amount`() = withTestApiApplication {
        val amount = BigDecimal.TEN.pow(20) - BigDecimal.ONE
        val sender = createAccount(amount)
        val receiver = createAccount(BigDecimal.ONE)
        val transaction = makeTransaction(sender, receiver, amount)

        runBlocking {
            withTimeout(10_000) {
                awaitTransactions()
            }
        }

        val updatedTransaction = getTransactionById(transaction.id)
        assertEquals(TransactionStatus.FAILED.name, updatedTransaction.status)
    }
}
