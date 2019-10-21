package com.awesoon.domain

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object Accounts : LongIdTable() {
    val balance = decimal("balance", precision = 30, scale = 10)
}

class Account(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Account>(Accounts)

    var balance by Accounts.balance
}
