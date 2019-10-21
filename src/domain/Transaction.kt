package com.awesoon.domain

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object Transactions : LongIdTable() {
    val fromId = reference("from_id", Accounts)
    val toId = reference("to_id", Accounts)
    val amount = decimal("balance", precision = 30, scale = 10)
    val status = varchar("status", 16)
}

class Transaction(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Transaction>(Transactions)

    var fromId by Transactions.fromId
    var from by Account referencedOn Transactions.fromId
    var toId by Transactions.toId
    var to by Account referencedOn Transactions.toId
    var amount by Transactions.amount
    var status by Transactions.status
}

enum class TransactionStatus {
    CREATED,
    FAILED,
    COMPLETED
}
