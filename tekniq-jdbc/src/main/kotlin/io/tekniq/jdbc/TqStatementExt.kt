@file:Suppress("TooManyFunctions")
package io.tekniq.jdbc

import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date

typealias ParamMap = Map<String, Any?>

fun parseNamedParameters(sql: String): Pair<String, List<String>> {
    val names = mutableListOf<String>()
    val normalizedSql = Regex("""(:[a-zA-Z_][a-zA-Z0-9_]*)""").replace(sql) {
        names.add(it.groupValues[1].drop(1))
        "?"
    }
    return normalizedSql to names
}

fun applyNamedParameters(stmt: PreparedStatement, sql: String, vararg params: Any?) {
    val (names) = parseNamedParameters(sql)
    if (names.isEmpty()) {
        stmt.applyParams(*params)
    } else {
        val first = params.getOrNull(0)
        if (first is Map<*, *>) {
            val values = names.map { name -> first[name] }
            stmt.applyParams(*values.toTypedArray())
        } else {
            stmt.applyParams(*params)
        }
    }
}

fun PreparedStatement.applyParams(vararg params: Any?) {
    params.forEachIndexed { index, param ->
        when (param) {
            is Time -> setTime(index + 1, param)
            is LocalTime -> setTime(index + 1, Time.valueOf(param))
            is java.sql.Date -> setDate(index + 1, param)
            is LocalDate -> setDate(index + 1, java.sql.Date.valueOf(param))
            is ZonedDateTime -> setTimestamp(index + 1, Timestamp.from(param.toInstant()))
            is LocalDateTime -> setTimestamp(index + 1, Timestamp.valueOf(param))
            is Date -> setTimestamp(index + 1, Timestamp(param.time))
            is Calendar -> setTimestamp(index + 1, Timestamp(param.timeInMillis))
            else -> setObject(index + 1, param)
        }
    }
}

fun PreparedStatement.setBooleanNull(index: Int, x: Boolean?) = if (x == null) setNull(index, Types.BOOLEAN) else setBoolean(index, x)
fun PreparedStatement.setByteNull(index: Int, x: Byte?) = if (x == null) setNull(index, Types.TINYINT) else setByte(index, x)
fun PreparedStatement.setShortNull(index: Int, x: Short?) = if (x == null) setNull(index, Types.SMALLINT) else setShort(index, x)
fun PreparedStatement.setIntNull(index: Int, x: Int?) = if (x == null) setNull(index, Types.INTEGER) else setInt(index, x)
fun PreparedStatement.setLongNull(index: Int, x: Long?) = if (x == null) setNull(index, Types.BIGINT) else setLong(index, x)
fun PreparedStatement.setFloatNull(index: Int, x: Float?) = if (x == null) setNull(index, Types.FLOAT) else setFloat(index, x)
fun PreparedStatement.setDoubleNull(index: Int, x: Double?) = if (x == null) setNull(index, Types.DOUBLE) else setDouble(index, x)

fun CallableStatement.setBooleanNull(paramName: String, x: Boolean?) = if (x == null) setNull(paramName, Types.BOOLEAN) else setBoolean(paramName, x)
fun CallableStatement.setByteNull(paramName: String, x: Byte?) = if (x == null) setNull(paramName, Types.TINYINT) else setByte(paramName, x)
fun CallableStatement.setShortNull(paramName: String, x: Short?) = if (x == null) setNull(paramName, Types.SMALLINT) else setShort(paramName, x)
fun CallableStatement.setIntNull(paramName: String, x: Int?) = if (x == null) setNull(paramName, Types.INTEGER) else setInt(paramName, x)
fun CallableStatement.setLongNull(paramName: String, x: Long?) = if (x == null) setNull(paramName, Types.BIGINT) else setLong(paramName, x)
fun CallableStatement.setFloatNull(paramName: String, x: Float?) = if (x == null) setNull(paramName, Types.FLOAT) else setFloat(paramName, x)
fun CallableStatement.setDoubleNull(paramName: String, x: Double?) = if (x == null) setNull(paramName, Types.DOUBLE) else setDouble(paramName, x)