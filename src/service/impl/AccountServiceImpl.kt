package com.awesoon.service.impl

import com.awesoon.domain.Account
import com.awesoon.domain.Accounts
import com.awesoon.domain.validateColumnScaleAndPrecision
import com.awesoon.domain.withDbContext
import com.awesoon.exception.AppException
import com.awesoon.service.AccountService
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import java.math.BigDecimal

class AccountServiceImpl : AccountService {
    override suspend fun createAccount(initialBalance: BigDecimal): Account {
        if (initialBalance < BigDecimal.ZERO) {
            throw AppException("invalid initial balance")
        }
        try {
            initialBalance.validateColumnScaleAndPrecision(Accounts.balance)
        } catch (e: IllegalArgumentException) {
            throw AppException("invalid initial balance")
        }
        return withDbContext {
            Account.new {
                balance = initialBalance
            }
        }
    }

    override suspend fun getTotalBalance(): BigDecimal = withDbContext {
        val balanceSum = Accounts.balance.sum()
        Accounts.slice(balanceSum).selectAll().first()[balanceSum] ?: BigDecimal.ZERO
    }

    override suspend fun getAccountById(id: Long): Account? = withDbContext {
        Account.findById(id)
    }
}
