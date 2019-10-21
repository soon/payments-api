package com.awesoon.dto.account

import com.awesoon.domain.Account
import java.math.BigDecimal

data class AccountDto(val id: Long, val balance: BigDecimal) {
    object ModelMapper {
        fun from(account: Account) = AccountDto(account.id.value, account.balance.stripTrailingZeros())
    }
}
