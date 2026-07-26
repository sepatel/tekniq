@file:Suppress("TooManyFunctions")
package io.tekniq.jdbc

import java.sql.ResultSet

operator fun ResultSet.get(columnLabel: String): Any? = getObject(columnLabel)
operator fun ResultSet.get(columnIndex: Int): Any? = getObject(columnIndex)

fun ResultSet.getBooleanNull(x: Int) = getBoolean(x).takeIf { !wasNull() }
fun ResultSet.getBooleanNull(x: String) = getBoolean(x).takeIf { !wasNull() }
fun ResultSet.getByteNull(x: Int) = getByte(x).takeIf { !wasNull() }
fun ResultSet.getByteNull(x: String) = getByte(x).takeIf { !wasNull() }
fun ResultSet.getShortNull(x: Int) = getShort(x).takeIf { !wasNull() }
fun ResultSet.getShortNull(x: String) = getShort(x).takeIf { !wasNull() }
fun ResultSet.getIntNull(x: Int) = getInt(x).takeIf { !wasNull() }
fun ResultSet.getIntNull(x: String) = getInt(x).takeIf { !wasNull() }
fun ResultSet.getLongNull(x: Int) = getLong(x).takeIf { !wasNull() }
fun ResultSet.getLongNull(x: String) = getLong(x).takeIf { !wasNull() }
fun ResultSet.getFloatNull(x: Int) = getFloat(x).takeIf { !wasNull() }
fun ResultSet.getFloatNull(x: String) = getFloat(x).takeIf { !wasNull() }
fun ResultSet.getDoubleNull(x: Int) = getDouble(x).takeIf { !wasNull() }
fun ResultSet.getDoubleNull(x: String) = getDouble(x).takeIf { !wasNull() }

fun ResultSet.getStringNull(x: Int) = getString(x).takeIf { !wasNull() }
fun ResultSet.getStringNull(x: String) = getString(x).takeIf { !wasNull() }
fun ResultSet.getDateNull(x: Int) = getDate(x).takeIf { !wasNull() }
fun ResultSet.getDateNull(x: String) = getDate(x).takeIf { !wasNull() }
fun ResultSet.getTimestampNull(x: Int) = getTimestamp(x).takeIf { !wasNull() }
fun ResultSet.getTimestampNull(x: String) = getTimestamp(x).takeIf { !wasNull() }
fun ResultSet.getTimeNull(x: Int) = getTime(x).takeIf { !wasNull() }
fun ResultSet.getTimeNull(x: String) = getTime(x).takeIf { !wasNull() }

inline fun ResultSet.forEach(action: ResultSet.() -> Unit) {
    while (next()) action()
}