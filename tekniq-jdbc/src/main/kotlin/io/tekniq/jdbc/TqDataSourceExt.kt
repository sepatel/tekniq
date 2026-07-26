@file:Suppress("unused", "NOTHING_TO_INLINE")

package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.Connection
import javax.sql.DataSource
import javax.sql.rowset.CachedRowSet

inline fun <T> DataSource.transaction(
    commitOnCompletion: Boolean = true,
    level: Int = Connection.TRANSACTION_READ_COMMITTED,
    boundary: Connection.() -> T
): T? = connection.use { conn ->
    conn.autoCommit = false
    conn.transactionIsolation = level
    runCatching { boundary(conn) }
        .onSuccess { if (commitOnCompletion) conn.commit() }
        .onFailure { if (commitOnCompletion) conn.rollback() }
        .getOrThrow()
}

inline fun <T> DataSource.call(sql: String, noinline action: (call: CallableStatement) -> T): T? = connection.use { conn ->
    conn.autoCommit = false
    runCatching { conn.call(sql, action) }
        .onSuccess { conn.commit() }
        .onFailure { conn.rollback() }
        .getOrThrow()
}

fun DataSource.select(sql: String): CachedRowSet = connection.use { it.select(sql) }

fun <T> DataSource.select(sql: String, vararg params: Any?, action: RowMapper<T>): Sequence<T> =
    connection.use { it.select(sql, *params, action = action) }

fun <T> DataSource.selectFirst(sql: String, vararg params: Any?, action: RowMapper<T>): T? =
    connection.use { it.selectFirst(sql, *params, action = action) }

fun DataSource.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun DataSource.insert(sql: String, vararg params: Any?): Int = update(sql, *params)

fun DataSource.update(sql: String, vararg params: Any?): Int = connection.use { conn ->
    conn.autoCommit = false
    runCatching { conn.update(sql, *params) }
        .onSuccess { conn.commit() }
        .onFailure { conn.rollback() }
        .getOrThrow()
}

fun DataSource.insertReturnKey(sql: String, vararg params: Any?): String? = connection.use { conn ->
    conn.autoCommit = false
    runCatching { conn.insertReturnKey(sql, *params) }
        .onSuccess { conn.commit() }
        .onFailure { conn.rollback() }
        .getOrThrow()
}