package io.tekniq.jdbc

import java.sql.ResultSet

fun ResultSet.getBooleanNull(x: Int) = returnNullable(getBoolean(x))
fun ResultSet.getBooleanNull(x: String) = returnNullable(getBoolean(x))
fun ResultSet.getByteNull(x: Int) = returnNullable(getByte(x))
fun ResultSet.getByteNull(x: String) = returnNullable(getByte(x))
fun ResultSet.getShortNull(x: Int) = returnNullable(getShort(x))
fun ResultSet.getShortNull(x: String) = returnNullable(getShort(x))
fun ResultSet.getIntNull(x: Int) = returnNullable(getInt(x))
fun ResultSet.getIntNull(x: String) = returnNullable(getInt(x))
fun ResultSet.getLongNull(x: Int) = returnNullable(getLong(x))
fun ResultSet.getLongNull(x: String) = returnNullable(getLong(x))
fun ResultSet.getFloatNull(x: Int) = returnNullable(getFloat(x))
fun ResultSet.getFloatNull(x: String) = returnNullable(getFloat(x))
fun ResultSet.getDoubleNull(x: Int) = returnNullable(getDouble(x))
fun ResultSet.getDoubleNull(x: String) = returnNullable(getDouble(x))

private fun <T> ResultSet.returnNullable(x: T): T? = when (wasNull()) {
    true -> null
    false -> x
}
