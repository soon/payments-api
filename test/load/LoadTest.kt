package com.awesoon.load

import com.awesoon.domain.TransactionStatus
import com.awesoon.dto.exception.AppExceptionDto
import com.awesoon.dto.transaction.NewTransactionDto
import com.awesoon.dto.transaction.TransactionDto
import com.awesoon.service.InstrumentedTransactionService
import com.awesoon.utils.awaitTransactions
import com.awesoon.utils.createAccount
import com.awesoon.utils.fromJson
import com.awesoon.utils.getTotalBalance
import com.awesoon.utils.getTransactionById
import com.awesoon.utils.postJson
import com.awesoon.utils.withTestApiApplication
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import java.math.BigDecimal
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

data class TransferMoneyTaskData(val dto: NewTransactionDto) {
    lateinit var status: HttpStatusCode
    lateinit var responseDto: TransactionDto
    lateinit var transaction: TransactionDto
    lateinit var errorMsg: String

    val isOk: Boolean
        get() = status == HttpStatusCode.OK
}

class CustomInstrumentedTransactionService : InstrumentedTransactionService {
    override fun onBeforeMoneyTransfer() {
        Thread.sleep(2000)
    }
}

class LoadTest {
    @Test
    fun `test balance`() = withTestApiApplication {
        generalLoadTest(totalAccounts = 10, initialBalance = BigDecimal(100), transactionsAmount = 10_000)
    }

    @Test
    fun `test transactions between two accounts`() = withTestApiApplication {
        generalLoadTest(totalAccounts = 2, initialBalance = BigDecimal(100), transactionsAmount = 1_000)
    }

    @Test
    fun `test transactions between two accounts with long transactions`() = withTestApiApplication({
        bind<InstrumentedTransactionService>(overrides = true) with singleton { CustomInstrumentedTransactionService() }
    }) {
        generalLoadTest(totalAccounts = 2, initialBalance = BigDecimal(100), transactionsAmount = 10)
    }

    private fun TestApplicationEngine.generalLoadTest(
        totalAccounts: Int,
        initialBalance: BigDecimal,
        transactionsAmount: Int
    ) {
        val tasksDispatcher = Executors.newFixedThreadPool(8).asCoroutineDispatcher()

        val accounts = (1..totalAccounts).map { createAccount(initialBalance) }
        val expectedBalance = initialBalance * totalAccounts.toBigDecimal()
        assertEquals(expectedBalance, getTotalBalance().value)

        val tasksData = (1..transactionsAmount).map {
            val fromAccount = accounts.random()
            var toAccount = accounts.random()
            while (fromAccount.id == toAccount.id) {
                toAccount = accounts.random()
            }
            val amount = (1..10).random()
            val dto = NewTransactionDto(fromAccount.id, toAccount.id, BigDecimal(amount))
            TransferMoneyTaskData(dto)
        }

        val tasksJobs = tasksData.map { task ->
            launch(tasksDispatcher) {
                withTimeout(1000) {
                    with(postJson("/api/money/transfer", task.dto).response) {
                        val status = status()!!
                        task.status = status
                        if (status == HttpStatusCode.OK) {
                            task.responseDto = content!!.fromJson()
                        } else {
                            val exceptionDto = content!!.fromJson<AppExceptionDto>()
                            task.errorMsg = exceptionDto.message
                        }
                    }
                }
            }
        }.toList()

        var prevPercentage = 0
        tasksJobs.forEachIndexed { idx, job ->
            runBlocking {
                job.join()
            }
            val percentage = (idx + 1) * 100 / transactionsAmount
            if (percentage % 10 == 0 && prevPercentage != percentage) {
                println("$percentage% of tasks completed")
                prevPercentage = percentage
            }
        }

        println("Awaiting transactions finishing...")

        runBlocking {
            withTimeout(10_000) {
                awaitTransactions()
            }
        }

        println("All transactions finished, refreshing statuses")

        tasksData.filter { it.isOk }.forEach {
            it.transaction = getTransactionById(it.responseDto.id)
        }

        val successfulRequests = tasksData.filter { it.status == HttpStatusCode.OK }
        val successfulRequestsCount = successfulRequests.count()
        println(
            "Successful requests: $successfulRequestsCount (${successfulRequestsCount * 100 / transactionsAmount}%)"
        )

        val successfulTransactions =
            tasksData.filter { it.isOk && it.transaction.status == TransactionStatus.COMPLETED.name }
        val successfulTransactionsCount = successfulTransactions.count()
        println(
            "Successful transactions: $successfulTransactionsCount " +
                "(${successfulTransactionsCount * 100 / successfulRequestsCount}%)"
        )

        val finalBalance = getTotalBalance().value
        assertEquals(expectedBalance, finalBalance)
        println("Final balance: $finalBalance")
    }
}
