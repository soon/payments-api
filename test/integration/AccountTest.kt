package com.awesoon.integration

import com.awesoon.dto.account.AccountDto
import com.awesoon.dto.account.NewAccountDto
import com.awesoon.dto.exception.AppExceptionDto
import com.awesoon.dto.exception.ValidationExceptionDto
import com.awesoon.utils.assertContentJsonIs
import com.awesoon.utils.assertStatusIs
import com.awesoon.utils.fromJson
import com.awesoon.utils.postJson
import com.awesoon.utils.withTestApiApplication
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

fun TestApplicationResponse.assertAccountBalanceIs(value: BigDecimal) {
    val account = content?.fromJson<AccountDto>()
    assertEquals(value, account?.balance)
}

class AccountTest {
    @Test
    fun `test account creation`() = withTestApiApplication {
        val call = postJson("/api/accounts", NewAccountDto(balance = BigDecimal(123)))
        with(call.response) {
            assertStatusIs(HttpStatusCode.OK)
            assertAccountBalanceIs(BigDecimal(123))
        }
    }

    @Test
    fun `test zero balance`() = withTestApiApplication {
        val call = postJson("/api/accounts", NewAccountDto(balance = BigDecimal.ZERO))
        with(call.response) {
            assertStatusIs(HttpStatusCode.OK)
            assertAccountBalanceIs(BigDecimal.ZERO)
        }
    }

    @Test
    fun `test account creation with negative balance`() = withTestApiApplication {
        val call = postJson("/api/accounts", NewAccountDto(balance = BigDecimal(-1)))
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                ValidationExceptionDto(
                    message = "Validation failed",
                    fieldErrors = mapOf("balance" to "Must not be negative.")
                )
            )
        }
    }

    @Test
    fun `test account creation with max scale`() = withTestApiApplication {
        val call = postJson("/api/accounts", NewAccountDto(balance = BigDecimal("1230.0000000001")))
        with(call.response) {
            assertStatusIs(HttpStatusCode.OK)
            assertAccountBalanceIs(BigDecimal("1230.0000000001"))
        }
    }

    @Test
    fun `test account creation with invalid scale`() = withTestApiApplication {
        val call = postJson("/api/accounts", NewAccountDto(balance = BigDecimal("0.00000000001")))
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "invalid initial balance"
                )
            )
        }
    }

    @Test
    fun `test max balance precision`() = withTestApiApplication {
        val balance = BigDecimal.TEN.pow(20) - BigDecimal("0.0000000001")
        val call = postJson("/api/accounts", NewAccountDto(balance = balance))
        with(call.response) {
            assertStatusIs(HttpStatusCode.OK)
            assertAccountBalanceIs(balance)
        }
    }

    @Test
    fun `test invalid balance precision`() = withTestApiApplication {
        val call = postJson("/api/accounts", NewAccountDto(balance = BigDecimal.TEN.pow(20)))
        with(call.response) {
            assertStatusIs(HttpStatusCode.BadRequest)
            assertContentJsonIs(
                AppExceptionDto(
                    message = "invalid initial balance"
                )
            )
        }
    }
}
