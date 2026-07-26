package io.tekniq.jdbc

import java.sql.ResultSet
import java.sql.Statement

/**
 * A lazy [Sequence] over a live JDBC cursor. It owns the [Statement] and [ResultSet] behind it, so
 * it must either be drained or closed:
 *
 * ```kotlin
 * connection.stream("SELECT id FROM account") { getLong("id") }.use { rows ->
 *     rows.first { it > 100 }
 * }
 * ```
 *
 * Draining closes automatically; `use {}` covers the partial-consumption paths (`first`, `take`,
 * `break`) that would otherwise hold the cursor open. Because the underlying cursor only moves
 * forward, iterating twice would silently skip rows, so a second pass throws instead.
 */
class TqRowSequence<T> internal constructor(
    private val stmt: Statement,
    private val rs: ResultSet,
    private val action: RowMapper<T>,
) : Sequence<T>, AutoCloseable {
    private var iterated = false
    private var closed = false

    override fun iterator(): Iterator<T> {
        check(!iterated) { "A TqRowSequence reads a forward-only cursor and cannot be iterated twice" }
        iterated = true
        return object : Iterator<T> {
            private var hasMore = rs.next().also { if (!it) close() }

            override fun hasNext() = hasMore

            override fun next(): T {
                if (!hasMore) throw NoSuchElementException()
                val row = action(rs)
                hasMore = rs.next()
                if (!hasMore) close()
                return row
            }
        }
    }

    /** Idempotent, so draining the sequence and then leaving a `use {}` block is safe. */
    override fun close() {
        if (closed) return
        closed = true
        // The statement must close even if the result set objects; drivers tie one to the other.
        try {
            rs.close()
        } finally {
            stmt.close()
        }
    }
}
