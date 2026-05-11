@file:Suppress("unused")

package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.rowset.CachedRowSet
import javax.sql.rowset.RowSetProvider

typealias RowMapper<T> = ResultSet.() -> T

fun Connection.select(sql: String): CachedRowSet = prepareStatement(sql)
    .use { stmt ->
        stmt.executeQuery().use { rs ->
            RowSetProvider.newFactory()
                .createCachedRowSet()
                .also { it.populate(rs) }
        }
    }

fun <T> Connection.select(sql: String, vararg params: Any?, action: RowMapper<T>): Sequence<T> {
    val (normalizedSql, names) = parseNamedParameters(sql)
    val paramValues = if (names.isEmpty()) params.toList() else {
        val paramMap = params.getOrNull(0) as? Map<String, Any?>
        paramMap?.let { map -> names.map { map[it] } } ?: params.toList()
    }
    return prepareStatement(normalizedSql).let { stmt ->
        stmt.applyParams(*paramValues.toTypedArray())
        stmt.closeOnCompletion()
        ResultSetSequence(stmt.executeQuery(), action)
    }
}

fun <T> Connection.selectFirst(sql: String, vararg params: Any?, action: RowMapper<T>): T? {
    val (normalizedSql, names) = parseNamedParameters(sql)
    val paramValues = if (names.isEmpty()) params.toList() else {
        val paramMap = params.getOrNull(0) as? Map<String, Any?>
        paramMap?.let { map -> names.map { map[it] } } ?: params.toList()
    }
    return prepareStatement(normalizedSql).use { stmt ->
        stmt.applyParams(*paramValues.toTypedArray())
        stmt.executeQuery().use { rs ->
            if (rs.next()) action(rs) else null
        }
    }
}

fun Connection.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.insert(sql: String, vararg params: Any?): Int = update(sql, *params)

fun Connection.update(sql: String, vararg params: Any?): Int {
    val (normalizedSql, names) = parseNamedParameters(sql)
    val paramValues = if (names.isEmpty()) params.toList() else {
        val paramMap = params.getOrNull(0) as? Map<String, Any?>
        paramMap?.let { map -> names.map { map[it] } } ?: params.toList()
    }
    return prepareStatement(normalizedSql).use { stmt ->
        stmt.applyParams(*paramValues.toTypedArray())
        stmt.executeUpdate()
    }
}

fun Connection.insertReturnKey(sql: String, vararg params: Any?): String? {
    val (normalizedSql, names) = parseNamedParameters(sql)
    val paramValues = if (names.isEmpty()) params.toList() else {
        val paramMap = params.getOrNull(0) as? Map<String, Any?>
        paramMap?.let { map -> names.map { map[it] } } ?: params.toList()
    }
    return prepareStatement(normalizedSql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
        stmt.applyParams(*paramValues.toTypedArray())
        stmt.executeUpdate()
        stmt.generatedKeys.use { rs ->
            if (rs.next()) rs.getString(1) else null
        }
    }
}

fun <T> Connection.call(sql: String, action: (call: CallableStatement) -> T): T? =
    prepareCall(sql).use { action(it) }