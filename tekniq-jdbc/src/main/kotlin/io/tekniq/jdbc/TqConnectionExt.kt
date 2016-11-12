package io.tekniq.jdbc

import java.sql.*
import java.time.*
import java.util.*
import java.util.Date

fun Connection.select(sql: String, vararg params: Any?, action: ResultSet.() -> Unit) {
    val stmt = prepareStatement(this, sql, *params)
    val rs = stmt.executeQuery()
    while (rs.next()) {
        action.invoke(rs)
    }
    rs.close()
    stmt.close()
}

fun <T> Connection.select(sql: String, vararg params: Any?, action: ResultSet.() -> T): List<T> {
    val list = mutableListOf<T>()
    val stmt = prepareStatement(this, sql, *params)
    val rs = stmt.executeQuery()
    while (rs.next()) {
        list.add(action.invoke(rs))
    }
    rs.close()
    stmt.close()
    return list
}

fun <T> Connection.selectOne(sql: String, vararg params: Any?, action: ResultSet.() -> T): T? {
    var value: T? = null
    val stmt = prepareStatement(this, sql, *params)

    params.forEachIndexed { i, any -> stmt.setObject(i + 1, any) }
    val rs = stmt.executeQuery()
    if (rs.next()) {
        value = action.invoke(rs)
    }
    rs.close()
    stmt.close()
    return value
}

fun Connection.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.insert(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.update(sql: String, vararg params: Any?): Int {
    val stmt = prepareStatement(sql)
    val result = stmt.executeUpdate()
    stmt.closeOnCompletion()
    return result
}

fun Connection.call(sql: String, action: CallableStatement.() -> Unit) {
    val stmt = prepareCall(sql)
    try {
        action.invoke(stmt)
    } finally {
        stmt.close()
    }
}

fun <T> Connection.call(sql: String, action: CallableStatement.() -> T): T? {
    val stmt = prepareCall(sql)
    try {
        return action.invoke(stmt)
    } finally {
        stmt.close()
    }
}

private fun prepareStatement(conn: Connection, sql: String, vararg params: Any?) = conn.prepareStatement(sql).apply {
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

