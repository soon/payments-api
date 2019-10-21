package com.awesoon.config.impl

import com.awesoon.config.DbManager
import com.awesoon.domain.Accounts
import com.awesoon.domain.Transactions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

open class DbManagerImpl : DbManager {
    override fun openDbConnection() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    }

    override fun getAllTables(): Array<Table> {
        return arrayOf(Accounts, Transactions)
    }

    override fun createTables() {
        transaction {
            SchemaUtils.create(*getAllTables())
        }
    }

    override fun dropTables() {
        transaction {
            SchemaUtils.drop(*getAllTables())
        }
    }
}
