@file:SuppressWarnings("TooManyFunctions")

package io.tekniq.jdbc

import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import java.util.Date

fun PreparedStatement.applyParams(vararg params: Any?) = params.forEachIndexed { i, any ->
    when (any) {
        is Time -> setTime(i + 1, any) // is also a java.util.Date so treat naturally instead
        is LocalTime -> setTime(i + 1, Time.valueOf(any))
        is java.sql.Date -> setDate(i + 1, any) // is also a java.util.Date so treat naturally instead
        is LocalDate -> setDate(i + 1, java.sql.Date.valueOf(any))
        is ZonedDateTime -> setTimestamp(i + 1, Timestamp(any.toInstant().toEpochMilli()))
        is LocalDateTime -> setTimestamp(i + 1, Timestamp.valueOf(any))
        is Date -> setTimestamp(i + 1, Timestamp(any.time))
        is Calendar -> setTimestamp(i + 1, Timestamp(any.timeInMillis))
        else -> setObject(i + 1, any)
    }
}


// Prepared Statement Extensions
fun PreparedStatement.setBooleanNull(index: Int, x: Boolean?) =
    if (x == null) setNull(index, Types.BOOLEAN)
    else setBoolean(index, x)

fun PreparedStatement.setByteNull(index: Int, x: Byte?) =
    if (x == null) setNull(index, Types.TINYINT)
    else setByte(index, x)

fun PreparedStatement.setShortNull(index: Int, x: Short?) =
    if (x == null) setNull(index, Types.SMALLINT)
    else setShort(index, x)

fun PreparedStatement.setIntNull(index: Int, x: Int?) =
    if (x == null) setNull(index, Types.INTEGER)
    else setInt(index, x)

fun PreparedStatement.setLongNull(index: Int, x: Long?) =
    if (x == null) setNull(index, Types.BIGINT)
    else setLong(index, x)

fun PreparedStatement.setFloatNull(index: Int, x: Float?) =
    if (x == null) setNull(index, Types.FLOAT)
    else setFloat(index, x)

fun PreparedStatement.setDoubleNull(index: Int, x: Double?) =
    if (x == null) setNull(index, Types.DOUBLE)
    else setDouble(index, x)


// Callable Statement Extensions
fun CallableStatement.setBooleanNull(paramName: String, x: Boolean?) =
    if (x == null) setNull(paramName, Types.BOOLEAN)
    else setBoolean(paramName, x)

fun CallableStatement.setByteNull(paramName: String, x: Byte?) =
    if (x == null) setNull(paramName, Types.TINYINT)
    else setByte(paramName, x)

fun CallableStatement.setShortNull(paramName: String, x: Short?) =
    if (x == null) setNull(paramName, Types.SMALLINT)
    else setShort(paramName, x)

fun CallableStatement.setIntNull(paramName: String, x: Int?) =
    if (x == null) setNull(paramName, Types.INTEGER)
    else setInt(paramName, x)

fun CallableStatement.setLongNull(paramName: String, x: Long?) =
    if (x == null) setNull(paramName, Types.BIGINT)
    else setLong(paramName, x)

fun CallableStatement.setFloatNull(paramName: String, x: Float?) =
    if (x == null) setNull(paramName, Types.FLOAT)
    else setFloat(paramName, x)

fun CallableStatement.setDoubleNull(paramName: String, x: Double?) =
    if (x == null) setNull(paramName, Types.DOUBLE)
    else setDouble(paramName, x)
