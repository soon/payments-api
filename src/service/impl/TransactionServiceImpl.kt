package com.awesoon.service.impl

import com.awesoon.domain.Account
import com.awesoon.domain.Accounts
import com.awesoon.domain.Transaction
import com.awesoon.domain.TransactionStatus
import com.awesoon.domain.Transactions
import com.awesoon.domain.findByIdForUpdate
import com.awesoon.domain.validateColumnScaleAndPrecision
import com.awesoon.domain.withDbContext
import com.awesoon.exception.AppException
import com.awesoon.exception.RetryableException
import com.awesoon.service.AccountService
import com.awesoon.service.InstrumentedTransactionService
import com.awesoon.service.TransactionService
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.math.BigDecimal

class TransactionServiceImpl(kodein: Kodein) : TransactionService {
    private val accountService: AccountService by kodein.instance()
    private val instrumentedTransactionService: InstrumentedTransactionService by kodein.instance()

    override suspend fun createTransaction(fromId: Long, toId: Long, amount: BigDecimal): Transaction {
        val from = accountService.getAccountById(fromId) ?: throw AppException("invalid fromId")
        val to = accountService.getAccountById(toId) ?: throw AppException("invalid toId")
        return createTransaction(from, to, amount)
    }

    private suspend fun createTransaction(from: Account, to: Account, amount: BigDecimal): Transaction {
        if (amount <= BigDecimal.ZERO) {
            throw AppException("invalid amount")
        }
        try {
            amount.validateColumnScaleAndPrecision(Transactions.amount)
        } catch (e: IllegalArgumentException) {
            throw AppException("invalid amount")
        }
        if (from.id == to.id) {
            throw AppException("same accounts")
        }
        if (from.balance < amount) {
            throw AppException("not enough money")
        }
        return withDbContext {
            Transaction.new {
                this.from = from
                this.to = to
                this.amount = amount
                status = TransactionStatus.CREATED.name
            }
        }
    }

    override suspend fun executeTransaction(txId: Long) = withDbContext {
        val tx = Transaction.findByIdForUpdate(txId) ?: return@withDbContext
        if (tx.status != TransactionStatus.CREATED.name) {
            return@withDbContext
        }
        try {
            doExecuteTransaction(tx)
        } catch (e: Exception) {
            if (e is RetryableException) {
                throw e
            }
            rollback()
            tx.status = TransactionStatus.FAILED.name
        }
    }

    private fun doExecuteTransaction(tx: Transaction) {
        if (tx.status != TransactionStatus.CREATED.name) {
            throw AppException("invalid status")
        }
        if (tx.amount <= BigDecimal.ZERO) {
            throw AppException("zero amount")
        }
        if (tx.fromId == tx.toId) {
            throw AppException("same accounts")
        }

        val fromAccount: Account?
        val toAccount: Account?

        if (tx.fromId < tx.toId) {
            fromAccount = Account.findByIdForUpdate(tx.fromId.value)
            toAccount = Account.findByIdForUpdate(tx.toId.value)
        } else {
            toAccount = Account.findByIdForUpdate(tx.toId.value)
            fromAccount = Account.findByIdForUpdate(tx.fromId.value)
        }

        if (fromAccount == null || toAccount == null) {
            throw AppException("invalid accounts")
        }
        if (fromAccount.balance < tx.amount) {
            throw AppException("not enough money")
        }
        val finalValue = toAccount.balance + tx.amount
        try {
            finalValue.validateColumnScaleAndPrecision(Accounts.balance)
        } catch (e: IllegalArgumentException) {
            throw AppException("invalid amount")
        }

        instrumentedTransactionService.onBeforeMoneyTransfer()

        changeAmount(fromAccount, -tx.amount)
        changeAmount(toAccount, tx.amount)

        tx.status = TransactionStatus.COMPLETED.name
    }

    private fun changeAmount(acc: Account, amount: BigDecimal) {
        assert(amount != BigDecimal.ZERO)
        Accounts.update({ Accounts.id eq acc.id }) {
            it[balance] = acc.balance + amount
        }
    }

    override suspend fun countPendingTransactions(): Int = withDbContext {
        Transactions.select {
            Transactions.status eq TransactionStatus.CREATED.name
        }.count()
    }

    override suspend fun getTransactionById(id: Long): Transaction? = withDbContext {
        Transaction.findById(id)
    }
}
