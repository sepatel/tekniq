@file:Suppress("unused", "NOTHING_TO_INLINE")

package io.tekniq.jdbc

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.rowset.CachedRowSet
import javax.sql.rowset.RowSetProvider

inline fun Connection.select(sql: String, vararg params: Any?): CachedRowSet = prepareStatement(sql)
    .use { stmt ->
        stmt.applyParams(*params)
        stmt.executeQuery().use { rs ->
            RowSetProvider.newFactory()
                .createCachedRowSet()
                .also { it.populate(rs) }
        }
    }

inline fun Connection.select(sql: String, vararg params: Any?, action: (rs: ResultSet) -> Unit) = prepareStatement(sql)
    .use { stmt ->
        stmt.applyParams(*params)
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                action.invoke(rs)
            }
        }
    }

inline fun <T> Connection.select(sql: String, vararg params: Any?, action: (rs: ResultSet) -> T): List<T> {
    val list = mutableListOf<T>()
    prepareStatement(sql).use { stmt ->
        stmt.applyParams(*params)
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                list.add(action.invoke(rs))
            }
        }
    }
    return list
}

inline fun <T> Connection.selectOne(sql: String, vararg params: Any?, action: (rs: ResultSet) -> T): T? {
    var value: T? = null
    prepareStatement(sql).use { stmt ->
        stmt.applyParams(*params)
        stmt.executeQuery().use { rs ->
            if (rs.next()) {
                value = action.invoke(rs)
            }
        }
    }
    return value
}

fun Connection.delete(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.insert(sql: String, vararg params: Any?): Int = update(sql, *params)
fun Connection.update(sql: String, vararg params: Any?): Int = prepareStatement(sql).use {
    it.applyParams(*params)
    it.executeUpdate()
}

@SuppressWarnings("NestedBlockDepth")
fun Connection.insertReturnKey(sql: String, vararg params: Any?): String? =
    prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
        stmt.applyParams(*params)
        val result = stmt.executeUpdate()
        var answer: String? = null
        if (result == 1) {
            stmt.generatedKeys.use { rs ->
                if (rs.next()) {
                    answer = rs.getString(1)
                }
            }
        }
        return answer
    }

inline fun <T> Connection.call(sql: String, action: (call: CallableStatement) -> T): T? =
    prepareCall(sql).use { action.invoke(it) }
