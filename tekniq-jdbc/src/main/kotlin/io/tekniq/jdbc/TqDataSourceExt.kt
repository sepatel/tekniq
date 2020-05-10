package io.tekniq.jdbc

import java.sql.*
import javax.sql.DataSource
import javax.sql.rowset.CachedRowSet

inline fun <T> DataSource.transaction(commitOnCompletion: Boolean = true, level: Int = Connection.TRANSACTION_READ_COMMITTED, boundary: Connection.() -> T): T? {
    connection.use { conn ->
        conn.autoCommit = false
        conn.transactionIsolation = level
        try {
            val result = boundary(conn)
            if (commitOnCompletion) {
                conn.commit()
            }
            return result
        } catch (e: Exception) {
            if (commitOnCompletion) {
                // control over commit/rollback is being handled externally
                conn.rollback()
            }
            throw e
        }
    }
}

inline fun <T> DataSource.call(sql: String, action: CallableStatement.() -> T): T? {
    connection.use { conn ->
        conn.autoCommit = false
        try {
            val x = conn.call(sql, action = action)
            conn.commit()
            return x
        } catch (e: Exception) {
            conn.rollback()
            throw e
        }
    }
}

inline fun DataSource.select(sql: String, vararg params: Any?): CachedRowSet =
        connection.use { it.select(sql, *params) }

inline fun DataSource.select(sql: String, vararg params: Any?, action: ResultSet.() -> Unit) =
        connection.use { it.select(sql, *params, action = action) }

inline fun <T> DataSource.select(sql: String, vararg params: Any?, action: ResultSet.() -> T): List<T> =
        connection.use { it.select(sql, *params, action = action) }

inline fun <T> DataSource.selectOne(sql: String, vararg params: Any?, action: ResultSet.() -> T): T? =
        connection.use { it.selectOne(sql, *params, action = action) }

inline fun DataSource.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
inline fun DataSource.insert(sql: String, vararg params: Any?): Int = update(sql, *params)
inline fun DataSource.update(sql: String, vararg params: Any?): Int = connection.use { conn ->
    conn.autoCommit = false
    try {
        val effected = conn.update(sql, *params)
        conn.commit()
        return effected
    } catch (e: Exception) {
        conn.rollback()
        throw e
    }
}
