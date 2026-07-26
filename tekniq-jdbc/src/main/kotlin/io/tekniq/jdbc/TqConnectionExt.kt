@file:Suppress("unused")

package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.rowset.CachedRowSet
import javax.sql.rowset.RowSetProvider

typealias RowMapper<T> = ResultSet.() -> T

fun Connection.select(sql: String, vararg params: Any?): CachedRowSet {
    val (normalizedSql, values) = resolveStatement(sql, params)
    return prepareStatement(normalizedSql).use { stmt ->
        stmt.applyParams(*values.toTypedArray())
        stmt.executeQuery().use { rs ->
            RowSetProvider.newFactory()
                .createCachedRowSet()
                .also { it.populate(rs) }
        }
    }
}

/** Reads every row into a list, releasing the statement and cursor before returning. */
fun <T> Connection.select(sql: String, vararg params: Any?, action: RowMapper<T>): List<T> =
    stream(sql, *params, action = action).use { it.toList() }

/**
 * Streams rows lazily. The returned sequence owns the statement and cursor, so drain it or wrap it
 * in `use {}` -- see [TqRowSequence]. Prefer [select] unless the result set is too large to hold.
 */
fun <T> Connection.stream(sql: String, vararg params: Any?, action: RowMapper<T>): TqRowSequence<T> {
    val (normalizedSql, values) = resolveStatement(sql, params)
    val stmt = prepareStatement(normalizedSql)
    // Nothing owns the statement until the sequence exists, so unwind it by hand until then.
    return runCatching {
        stmt.applyParams(*values.toTypedArray())
        TqRowSequence(stmt, stmt.executeQuery(), action)
    }.onFailure { stmt.close() }.getOrThrow()
}

fun <T> Connection.selectFirst(sql: String, vararg params: Any?, action: RowMapper<T>): T? {
    val (normalizedSql, values) = resolveStatement(sql, params)
    return prepareStatement(normalizedSql).use { stmt ->
        stmt.applyParams(*values.toTypedArray())
        stmt.executeQuery().use { rs ->
            if (rs.next()) action(rs) else null
        }
    }
}

fun Connection.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.insert(sql: String, vararg params: Any?): Int = update(sql, *params)

fun Connection.update(sql: String, vararg params: Any?): Int {
    val (normalizedSql, values) = resolveStatement(sql, params)
    return prepareStatement(normalizedSql).use { stmt ->
        stmt.applyParams(*values.toTypedArray())
        stmt.executeUpdate()
    }
}

/** Returns the generated key, or null unless the statement affected exactly one row. */
fun Connection.insertReturnKey(sql: String, vararg params: Any?): String? {
    val (normalizedSql, values) = resolveStatement(sql, params)
    return prepareStatement(normalizedSql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
        stmt.applyParams(*values.toTypedArray())
        if (stmt.executeUpdate() != 1) {
            null
        } else {
            stmt.generatedKeys.use { rs ->
                if (rs.next()) rs.getString(1) else null
            }
        }
    }
}

fun <T> Connection.call(sql: String, action: (call: CallableStatement) -> T): T? =
    prepareCall(sql).use { action(it) }
