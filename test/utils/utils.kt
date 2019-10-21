package com.awesoon.utils

import com.awesoon.api
import com.awesoon.config.DbManager
import com.awesoon.config.impl.DbManagerImpl
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.jetbrains.exposed.sql.Database
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import kotlin.test.assertEquals

fun TestApplicationEngine.postJson(
    uri: String,
    body: Any
): TestApplicationCall = handleRequest(HttpMethod.Post, uri) {
    addHeader("Content-Type", "application/json")
    val mapper = jacksonObjectMapper()
    if (body is String) {
        setBody(body)
    } else {
        setBody(mapper.writeValueAsString(body))
    }
}

inline fun <reified T : Any> String.fromJson(): T {
    val mapper = jacksonObjectMapper()
    return mapper.readValue(this)
}

inline fun <reified T : Any> TestApplicationCall.fromJson(): T {
    return response.content!!.fromJson()
}

inline fun <reified T : Any> TestApplicationResponse.assertContentJsonIs(value: T) {
    assertEquals(value, content!!.fromJson())
}

fun TestApplicationResponse.assertStatusIs(value: HttpStatusCode) {
    assertEquals(value, status())
}

object TestDbManager : DbManagerImpl() {
    private var db: Database? = null

    override fun openDbConnection() {
        if (db == null) {
            db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        }
    }
}

fun <R> withTestApiApplication(
    kodeinOverrides: Kodein.MainBuilder.() -> Unit = {},
    test: TestApplicationEngine.() -> R
) = withTestApplication({
    api(kodeinOverrides = {
        bind<DbManager>(overrides = true) with singleton { TestDbManager }
        kodeinOverrides()
    })
}) {
    try {
        test()
    } finally {
        TestDbManager.dropTables()
    }
}

fun <R> withTestApiApplication(test: TestApplicationEngine.() -> R) = withTestApiApplication({}, test)
