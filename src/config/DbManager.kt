package com.awesoon.config

import org.jetbrains.exposed.sql.Table

interface DbManager {
    fun openDbConnection()
    fun getAllTables(): Array<Table>
    fun createTables()
    fun dropTables()
}
