@file:Suppress("unused")

package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.Connection
import javax.sql.DataSource
import javax.sql.rowset.CachedRowSet

/**
 * Runs [boundary] against a pooled connection, committing on success and rolling back on failure.
 *
 * A non-local `return` out of [boundary] skips the commit and discards the work, which is usually
 * not what you want -- return a value instead.
 */
inline fun <T> DataSource.transaction(
    commitOnCompletion: Boolean = true,
    level: Int = Connection.TRANSACTION_READ_COMMITTED,
    boundary: Connection.() -> T
): T = connection.use { conn ->
    conn.autoCommit = false
    conn.transactionIsolation = level
    conn.commitOrRollback(commitOnCompletion) { boundary(conn) }
}

fun <T> DataSource.call(sql: String, action: (call: CallableStatement) -> T): T? = connection.use { conn ->
    conn.autoCommit = false
    conn.commitOrRollback { conn.call(sql, action) }
}

fun DataSource.select(sql: String, vararg params: Any?): CachedRowSet =
    connection.use { it.select(sql, *params) }

/**
 * Reads every row into a list. There is deliberately no lazy `DataSource` variant: the connection
 * would have to be released before the caller ever read a row, which returns a live cursor to the
 * pool. Stream from a connection you hold instead -- `connection.use { it.stream(...) }`.
 */
fun <T> DataSource.select(sql: String, vararg params: Any?, action: RowMapper<T>): List<T> =
    connection.use { it.select(sql, *params, action = action) }

fun <T> DataSource.selectFirst(sql: String, vararg params: Any?, action: RowMapper<T>): T? =
    connection.use { it.selectFirst(sql, *params, action = action) }

fun DataSource.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun DataSource.insert(sql: String, vararg params: Any?): Int = update(sql, *params)

fun DataSource.update(sql: String, vararg params: Any?): Int = connection.use { conn ->
    conn.autoCommit = false
    conn.commitOrRollback { conn.update(sql, *params) }
}

fun DataSource.insertReturnKey(sql: String, vararg params: Any?): String? = connection.use { conn ->
    conn.autoCommit = false
    conn.commitOrRollback { conn.insertReturnKey(sql, *params) }
}

/**
 * The commit belongs inside the guarded region: a deferred constraint or a dropped socket can fail
 * at commit time, and that has to roll back rather than hand a dirty connection back to the pool.
 */
@PublishedApi
internal inline fun <T> Connection.commitOrRollback(commit: Boolean = true, body: () -> T): T =
    runCatching { body().also { if (commit) this.commit() } }
        .onFailure { failure ->
            // A failing rollback must not replace the exception that caused it.
            if (commit) runCatching { rollback() }.onFailure(failure::addSuppressed)
        }
        .getOrThrow()
