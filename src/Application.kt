package com.awesoon

import com.awesoon.config.DbManager
import com.awesoon.config.impl.DbManagerImpl
import com.awesoon.domain.Account
import com.awesoon.domain.retryIfPossible
import com.awesoon.dto.account.AccountDto
import com.awesoon.dto.account.NewAccountDto
import com.awesoon.dto.exception.AppExceptionDto
import com.awesoon.dto.exception.ValidationExceptionDto
import com.awesoon.dto.stats.account.TotalBalanceDto
import com.awesoon.dto.stats.transaction.PendingTransactionsCountDto
import com.awesoon.dto.transaction.NewTransactionDto
import com.awesoon.dto.transaction.TransactionDto
import com.awesoon.exception.AppException
import com.awesoon.exception.ObjectNotFoundException
import com.awesoon.exception.ValidationException
import com.awesoon.service.AccountService
import com.awesoon.service.InstrumentedTransactionService
import com.awesoon.service.TransactionService
import com.awesoon.service.impl.AccountServiceImpl
import com.awesoon.service.impl.InstrumentedTransactionServiceImpl
import com.awesoon.service.impl.TransactionServiceImpl
import com.fasterxml.jackson.core.JsonGenerator
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.routing
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.kodein.di.ktor.kodein
import java.math.BigDecimal
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@kotlin.jvm.JvmOverloads
fun Application.api(kodeinOverrides: Kodein.MainBuilder.() -> Unit = {}) {
    setupApp(kodeinOverrides)

    val dbManager by kodein().instance<DbManager>()
    val accountService by kodein().instance<AccountService>()
    val transactionService by kodein().instance<TransactionService>()

    dbManager.openDbConnection()
    dbManager.createTables()

    routing {
        post<Api.Money.Transfer> {
            val transactionDto = call.receive(NewTransactionDto::class)
            val transaction = transactionService.createTransaction(
                transactionDto.fromId,
                transactionDto.toId,
                transactionDto.amount
            )
            call.respond(TransactionDto.ModelMapper.from(transaction))
            launch {
                retryIfPossible(Duration.ofMillis(100)) {
                    transactionService.executeTransaction(transaction.id.value)
                }
            }
        }

        post<Api.Accounts> {
            val accountDto = call.receive(NewAccountDto::class)
            if (accountDto.balance < BigDecimal.ZERO) {
                throw ValidationException(fieldErrors = mapOf("balance" to "Must not be negative."))
            }
            val account = accountService.createAccount(accountDto.balance)
            call.respond(AccountDto.ModelMapper.from(account))
        }

        get<Api.Internal.Transactions.Pending> {
            val count = transactionService.countPendingTransactions()
            call.respond(PendingTransactionsCountDto(count))
        }

        get<Api.Internal.Transaction> { path ->
            val account = transactionService.getTransactionById(path.id)
                ?: throw ObjectNotFoundException.ofType<Account>()
            call.respond(TransactionDto.ModelMapper.from(account))
        }

        get<Api.Internal.Account> { path ->
            val account =
                accountService.getAccountById(path.id) ?: throw ObjectNotFoundException.ofType<Account>()
            call.respond(AccountDto.ModelMapper.from(account))
        }

        get<Api.Internal.Accounts.Balance.Total> {
            val balance = accountService.getTotalBalance()
            call.respond(TotalBalanceDto(balance.stripTrailingZeros()))
        }
    }
}

private fun Application.setupApp(kodeinOverrides: Kodein.MainBuilder.() -> Unit) {
    install(Locations)
    install(ContentNegotiation) {
        jackson {
            configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
        }
    }
    install(StatusPages) {
        exception<ValidationException> {
            call.respond(HttpStatusCode.BadRequest, ValidationExceptionDto.ModelMapper.from(it))
        }
        exception<AppException> {
            call.respond(HttpStatusCode.BadRequest, AppExceptionDto.ModelMapper.from(it))
        }
    }
    kodein {
        bind<DbManager>() with singleton { DbManagerImpl() }
        bind<AccountService>() with singleton { AccountServiceImpl() }
        bind<TransactionService>() with singleton { TransactionServiceImpl(kodein) }
        bind<InstrumentedTransactionService>() with singleton { InstrumentedTransactionServiceImpl() }
        kodeinOverrides()
    }
}

@Location("/api")
class Api {
    @Location("/money")
    class Money {
        @Location("/transfer")
        class Transfer
    }

    @Location("/accounts")
    class Accounts

    @Location("/internal")
    class Internal {
        @Location("/transactions")
        class Transactions {
            @Location("/pending")
            class Pending
        }

        @Location("/transactions/{id}")
        data class Transaction(val id: Long)

        @Location("/accounts")
        class Accounts {
            @Location("/balance")
            class Balance {
                @Location("/total")
                class Total
            }
        }

        @Location("/accounts/{id}")
        data class Account(val id: Long)
    }
}
