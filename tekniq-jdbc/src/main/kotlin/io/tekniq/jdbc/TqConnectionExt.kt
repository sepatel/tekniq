@file:Suppress("unused", "NOTHING_TO_INLINE")

package io.tekniq.jdbc

import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import java.util.Date
import javax.sql.rowset.CachedRowSet
import javax.sql.rowset.RowSetProvider

inline fun Connection.select(sql: String, vararg params: Any?): CachedRowSet = prepareStatement(sql)
    .also { applyParams(it, *params) }
    .use { stmt ->
        stmt.executeQuery().use { rs ->
            RowSetProvider.newFactory()
                .createCachedRowSet()
                .also { it.populate(rs) }
        }
    }

inline fun Connection.select(sql: String, vararg params: Any?, action: ResultSet.() -> Unit) = prepareStatement(sql)
    .also { applyParams(it, *params) }
    .use { stmt ->
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                action.invoke(rs)
            }
        }
    }

inline fun <T> Connection.select(sql: String, vararg params: Any?, action: ResultSet.() -> T): List<T> {
    val list = mutableListOf<T>()
    prepareStatement(sql)
        .also { applyParams(it, *params) }
        .use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(action.invoke(rs))
                }
            }
        }
    return list
}

inline fun <T> Connection.selectOne(sql: String, vararg params: Any?, action: ResultSet.() -> T): T? {
    var value: T? = null
    prepareStatement(sql)
        .also { applyParams(it, *params) }
        .use { stmt ->
            params.forEachIndexed { i, any -> stmt.setObject(i + 1, any) }
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    value = action.invoke(rs)
                }
            }
        }
    return value
}

fun Connection.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.insert(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.update(sql: String, vararg params: Any?): Int = prepareStatement(sql)
    .also { applyParams(it, *params) }
    .use { it.executeUpdate() }

fun Connection.insertReturnKey(sql: String, vararg params: Any?): String? =
    prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        .also { applyParams(it, *params) }
        .use { stmt ->
            val result = stmt.executeUpdate()
            var answer: String? = null
            if (result == 1) {
                stmt.generatedKeys.use { rs ->
                    if (rs.next()) {
                        answer = rs.getString(1)
                    }
                }
            }
            return answer
        }

inline fun <T> Connection.call(sql: String, action: CallableStatement.() -> T): T? =
    prepareCall(sql).use { action.invoke(it) }

fun applyParams(stmt: PreparedStatement, vararg params: Any?) = stmt.also {
    params.forEachIndexed { i, any ->
        when (any) {
            is Time -> it.setTime(i + 1, any) // is also a java.util.Date so treat naturally instead
            is LocalTime -> it.setTime(i + 1, Time.valueOf(any))
            is java.sql.Date -> it.setDate(i + 1, any) // is also a java.util.Date so treat naturally instead
            is LocalDate -> it.setDate(i + 1, java.sql.Date.valueOf(any))
            is ZonedDateTime -> it.setTimestamp(i + 1, Timestamp(any.toInstant().toEpochMilli()))
            is LocalDateTime -> it.setTimestamp(i + 1, Timestamp.valueOf(any))
            is Date -> it.setTimestamp(i + 1, Timestamp(any.time))
            is Calendar -> it.setTimestamp(i + 1, Timestamp(any.timeInMillis))
            else -> it.setObject(i + 1, any)
        }
    }
}

