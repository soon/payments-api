package com.awesoon.domain

import com.awesoon.exception.RetryableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.delay
import org.h2.jdbc.JdbcSQLTimeoutException
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.DecimalColumnType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

fun <ID : Comparable<ID>, E : Entity<ID>> EntityClass<ID, E>.findByIdForUpdate(id: ID): E? = try {
    find { table.id eq id }.forUpdate().firstOrNull()
} catch (e: ExposedSQLException) {
    val cause = e.cause
    if (cause is JdbcSQLTimeoutException && cause.originalMessage.contains("Timeout trying to lock table")) {
        throw RetryableException(cause = e)
    }
    throw e
}

suspend fun <T> withDbContext(statement: suspend Transaction.() -> T) =
    suspendedTransactionAsync(Dispatchers.IO, statement = statement).await()

suspend fun <T> retryIfPossible(delayDuration: Duration, block: suspend () -> T): T {
    while (true) {
        try {
            return block()
        } catch (e: RetryableException) {
            delay(delayDuration + Duration.ofMillis((10..500L).random()))
        }
    }
}

fun BigDecimal.validateColumnScaleAndPrecision(column: Column<BigDecimal>) {
    val decimalColType = column.columnType as DecimalColumnType
    val copy = setScale(decimalColType.scale, RoundingMode.HALF_UP)
    require(copy.compareTo(this) == 0)
    require(copy.scale() <= decimalColType.scale)
    require(copy.precision() <= decimalColType.precision)
}
