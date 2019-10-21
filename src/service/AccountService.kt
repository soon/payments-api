package com.awesoon.service

import com.awesoon.domain.Account
import java.math.BigDecimal

interface AccountService {
    suspend fun createAccount(initialBalance: BigDecimal): Account
    suspend fun getTotalBalance(): BigDecimal
    suspend fun getAccountById(id: Long): Account?
}
