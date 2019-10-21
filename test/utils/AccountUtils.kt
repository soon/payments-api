package com.awesoon.utils

import com.awesoon.dto.account.AccountDto
import com.awesoon.dto.account.NewAccountDto
import com.awesoon.dto.stats.account.TotalBalanceDto
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import java.math.BigDecimal

fun TestApplicationEngine.createAccount(balance: BigDecimal): AccountDto =
    postJson("/api/accounts", NewAccountDto(balance = balance)).fromJson()

fun TestApplicationEngine.getAccountById(id: Long): AccountDto =
    handleRequest(HttpMethod.Get, "/api/internal/accounts/$id").fromJson()

fun TestApplicationEngine.getTotalBalance(): TotalBalanceDto =
    handleRequest(HttpMethod.Get, "/api/internal/accounts/balance/total").fromJson()
