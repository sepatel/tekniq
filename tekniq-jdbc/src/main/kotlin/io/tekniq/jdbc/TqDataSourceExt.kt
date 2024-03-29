@file:Suppress("unused", "NOTHING_TO_INLINE")

package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource
import javax.sql.rowset.CachedRowSet

inline fun <T> DataSource.transaction(
    commitOnCompletion: Boolean = true,
    level: Int = Connection.TRANSACTION_READ_COMMITTED,
    boundary: Connection.() -> T
): T? {
    connection.use { conn ->
        conn.autoCommit = false
        conn.transactionIsolation = level
        try {
            val result = boundary(conn)
            if (commitOnCompletion) {
                conn.commit()
            }
            return result
        } catch (@SuppressWarnings("TooGenericExceptionCaught") e: Exception) {
            if (commitOnCompletion) {
                // control over commit/rollback is being handled externally
                conn.rollback()
            }
            throw e
        }
    }
}

inline fun <T> DataSource.call(sql: String, action: (call: CallableStatement) -> T): T? {
    connection.use { conn ->
        conn.autoCommit = false
        try {
            val x = conn.call(sql, action = action)
            conn.commit()
            return x
        } catch (@SuppressWarnings("TooGenericExceptionCaught") e: Exception) {
            conn.rollback()
            throw e
        }
    }
}

inline fun DataSource.select(sql: String, vararg params: Any?): CachedRowSet =
    connection.use { it.select(sql, *params) }

inline fun DataSource.select(sql: String, vararg params: Any?, action: (rs: ResultSet) -> Unit) =
    connection.use { it.select(sql, *params, action = action) }

inline fun <T> DataSource.select(sql: String, vararg params: Any?, action: (rs: ResultSet) -> T): List<T> =
    connection.use { it.select(sql, *params, action = action) }

inline fun <T> DataSource.selectOne(sql: String, vararg params: Any?, action: (rs: ResultSet) -> T): T? =
    connection.use { it.selectOne(sql, *params, action = action) }

inline fun DataSource.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
inline fun DataSource.insert(sql: String, vararg params: Any?): Int = update(sql, *params)

inline fun DataSource.update(sql: String, vararg params: Any?): Int = connection.use { conn ->
    conn.autoCommit = false
    try {
        val effected = conn.update(sql, *params)
        conn.commit()
        return effected
    } catch (@SuppressWarnings("TooGenericExceptionCaught") e: Exception) {
        conn.rollback()
        throw e
    }
}

inline fun DataSource.insertReturnKey(sql: String, vararg params: Any?): String? = connection.use { conn ->
    conn.autoCommit = false
    try {
        val effected = conn.insertReturnKey(sql, *params)
        conn.commit()
        return effected
    } catch (@SuppressWarnings("TooGenericExceptionCaught") e: Exception) {
        conn.rollback()
        throw e
    }
}
