package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

fun <T> DataSource.transaction(commitOnCompletion: Boolean = true, level: Int = Connection.TRANSACTION_READ_COMMITTED, boundary: Connection.() -> T): T? {
    val conn = connection
    try {
        conn.autoCommit = false
        conn.transactionIsolation = level
        val result = boundary.invoke(conn)
        if (commitOnCompletion) {
            conn.commit()
        }
        return result
    } finally {
        conn.close()
    }
}

fun DataSource.call(sql: String, action: CallableStatement.() -> Unit) {
    val conn = connection
    try {
        conn.autoCommit = false
        conn.call(sql, action = action)
        conn.commit()
    } finally {
        conn.close()
    }
}

fun <T> DataSource.call(sql: String, action: CallableStatement.() -> T): T? {
    val conn = connection
    try {
        conn.autoCommit = false
        val x = conn.call(sql, action = action)
        conn.commit()
        return x
    } finally {
        conn.close()
    }
}

fun DataSource.select(sql: String, vararg params: Any?, action: ResultSet.() -> Unit) {
    val conn = connection
    try {
        conn.select(sql, *params, action = action)
    } finally {
        conn.close()
    }
}

fun <T> DataSource.select(sql: String, vararg params: Any?, action: ResultSet.() -> T): List<T> {
    val conn = connection
    try {
        return conn.select(sql, *params, action = action)
    } finally {
        conn.close()
    }
}

fun <T> DataSource.selectOne(sql: String, vararg params: Any?, action: ResultSet.() -> T): T? {
    val conn = connection
    try {
        return conn.selectOne(sql, *params, action = action)
    } finally {
        conn.close()
    }
}

fun DataSource.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun DataSource.insert(sql: String, vararg params: Any?): Int = update(sql, *params)
fun DataSource.update(sql: String, vararg params: Any?): Int {
    val conn = connection
    try {
        conn.autoCommit = false
        val effected = conn.update(sql, *params)
        conn.commit()
        return effected
    } finally {
        conn.close()
    }
}

