package io.tekniq.jdbc

import java.sql.*
import java.time.*
import java.util.*
import java.util.Date

inline fun Connection.select(sql: String, vararg params: Any?, action: ResultSet.() -> Unit)
        = prepareStatement(sql)
        .apply { applyParams(this, *params) }
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
            .apply { applyParams(this, *params) }
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
            .apply { applyParams(this, *params) }
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
fun Connection.update(sql: String, vararg params: Any?): Int
        = prepareStatement(sql)
        .apply { applyParams(this, *params) }
        .use { it.executeUpdate() }

fun Connection.insertReturnKey(sql: String, vararg params: Any?): String?
        = prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        .apply { applyParams(this, *params) }
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

inline fun <T> Connection.call(sql: String, action: CallableStatement.() -> T): T?
        = prepareCall(sql).use { action.invoke(it) }

fun applyParams(stmt: PreparedStatement, vararg params: Any?) = stmt.apply {
    params.forEachIndexed { i, any ->
        when (any) {
            is Time -> setTime(i + 1, any) // is also a java.util.Date so treat naturally instead
            is LocalTime -> setTime(i + 1, Time(any.hour, any.minute, any.second))
            is java.sql.Date -> setDate(i + 1, any) // is also a java.util.Date so treat naturally instead
            is LocalDate -> setDate(i + 1, java.sql.Date(any.year, any.monthValue - 1, any.dayOfMonth))
            is ZonedDateTime -> setTimestamp(i + 1, Timestamp(any.toInstant().toEpochMilli()))
            is LocalDateTime -> setTimestamp(i + 1, Timestamp(any.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            is Date -> setTimestamp(i + 1, Timestamp(any.time))
            is Calendar -> setTimestamp(i + 1, Timestamp(any.timeInMillis))
            else -> setObject(i + 1, any)
        }
    }
}

