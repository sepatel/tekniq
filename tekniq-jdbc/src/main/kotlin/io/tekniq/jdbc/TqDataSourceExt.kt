package io.tekniq.jdbc

import java.sql.*
import javax.sql.DataSource

inline fun <T> DataSource.transaction(commitOnCompletion: Boolean = true, level: Int = Connection.TRANSACTION_READ_COMMITTED, boundary: Connection.() -> T): T? {
    connection.use { conn ->
        conn.autoCommit = false
        conn.transactionIsolation = level
        val result = boundary.invoke(conn)
        if (commitOnCompletion) {
            conn.commit()
        }
        return result
    }
}

inline fun <T> DataSource.call(sql: String, action: CallableStatement.() -> T): T? {
    connection.use { conn ->
        conn.autoCommit = false
        val x = conn.call(sql, action = action)
        conn.commit()
        return x
    }
}

inline fun DataSource.select(sql: String, vararg params: Any?, action: ResultSet.() -> Unit)
        = connection.use { it.select(sql, *params, action = action) }

inline fun <T> DataSource.select(sql: String, vararg params: Any?, action: ResultSet.() -> T): List<T>
        = connection.use { it.select(sql, *params, action = action) }

fun <T> DataSource.selectOne(sql: String, vararg params: Any?, action: ResultSet.() -> T): T?
        = connection.use { it.selectOne(sql, *params, action = action) }

fun DataSource.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun DataSource.insert(sql: String, vararg params: Any?): Int = update(sql, *params)
fun DataSource.update(sql: String, vararg params: Any?): Int {
    connection.use { conn ->
        conn.autoCommit = false
        val effected = conn.update(sql, *params)
        conn.commit()
        return effected
    }
}

