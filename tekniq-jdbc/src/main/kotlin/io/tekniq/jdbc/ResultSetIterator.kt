package io.tekniq.jdbc

import java.sql.ResultSet

class ResultSetIterator<T>(private val rs: ResultSet, private val action: (ResultSet) -> T) : Iterator<T> {
    private var hasMore = rs.next()

    override fun hasNext(): Boolean = hasMore

    override fun next(): T {
        if (!hasMore) throw NoSuchElementException()
        val result = action(rs)
        hasMore = rs.next()
        if (!hasMore) {
            rs.close()
        }
        return result
    }
}
