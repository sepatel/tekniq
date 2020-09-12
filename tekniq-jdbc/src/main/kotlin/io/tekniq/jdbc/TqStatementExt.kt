@file:Suppress("unused")

package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.Types

// Prepared Statement Extensions
fun PreparedStatement.setBooleanNull(index: Int, x: Boolean?) = if (x == null) {
    setNull(index, Types.BOOLEAN)
} else {
    setBoolean(index, x)
}

fun PreparedStatement.setByteNull(index: Int, x: Byte?) = if (x == null) {
    setNull(index, Types.TINYINT)
} else {
    setByte(index, x)
}

fun PreparedStatement.setShortNull(index: Int, x: Short?) = if (x == null) {
    setNull(index, Types.SMALLINT)
} else {
    setShort(index, x)
}

fun PreparedStatement.setIntNull(index: Int, x: Int?) = if (x == null) {
    setNull(index, Types.INTEGER)
} else {
    setInt(index, x)
}

fun PreparedStatement.setLongNull(index: Int, x: Long?) = if (x == null) {
    setNull(index, Types.BIGINT)
} else {
    setLong(index, x)
}

fun PreparedStatement.setFloatNull(index: Int, x: Float?) = if (x == null) {
    setNull(index, Types.FLOAT)
} else {
    setFloat(index, x)
}

fun PreparedStatement.setDoubleNull(index: Int, x: Double?) = if (x == null) {
    setNull(index, Types.DOUBLE)
} else {
    setDouble(index, x)
}


// Callable Statement Extensions
fun CallableStatement.setBooleanNull(paramName: String, x: Boolean?) = if (x == null) {
    setNull(paramName, Types.BOOLEAN)
} else {
    setBoolean(paramName, x)
}

fun CallableStatement.setByteNull(paramName: String, x: Byte?) = if (x == null) {
    setNull(paramName, Types.TINYINT)
} else {
    setByte(paramName, x)
}

fun CallableStatement.setShortNull(paramName: String, x: Short?) = if (x == null) {
    setNull(paramName, Types.SMALLINT)
} else {
    setShort(paramName, x)
}

fun CallableStatement.setIntNull(paramName: String, x: Int?) = if (x == null) {
    setNull(paramName, Types.INTEGER)
} else {
    setInt(paramName, x)
}

fun CallableStatement.setLongNull(paramName: String, x: Long?) = if (x == null) {
    setNull(paramName, Types.BIGINT)
} else {
    setLong(paramName, x)
}

fun CallableStatement.setFloatNull(paramName: String, x: Float?) = if (x == null) {
    setNull(paramName, Types.FLOAT)
} else {
    setFloat(paramName, x)
}

fun CallableStatement.setDoubleNull(paramName: String, x: Double?) = if (x == null) {
    setNull(paramName, Types.DOUBLE)
} else {
    setDouble(paramName, x)
}

