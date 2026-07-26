@file:Suppress("unused")

/**
 * Compile-time signposts for API removed in 0.23.0. These exist purely so stale call sites fail to
 * build with a pointed message instead of silently resolving to something that behaves differently.
 * Delete the whole file in the next major.
 *
 * Three changes could not be signposted, because the replacement shares a name and signature with
 * what it replaces. They are listed here so this file is the single place to look:
 *
 * - `Connection.select`/`selectFirst`/`stream` and `ResultSet.forEach` now take a `ResultSet`
 *   receiver ([RowMapper]) rather than a parameter, so `{ it.getString(1) }` becomes
 *   `{ getString(1) }`. Old lambdas fail on an unresolved `it`.
 * - `DataSource.select(sql, params, action)` returns `List<T>`, not `Sequence<T>`.
 * - `ResultSetIterator` is gone; [TqRowSequence] owns iteration and closing together.
 */
package io.tekniq.jdbc

import java.sql.Connection
import javax.sql.DataSource

@Deprecated(
    "Renamed to selectFirst",
    ReplaceWith("selectFirst(sql, *params, action = action)"),
    DeprecationLevel.ERROR,
)
fun <T> Connection.selectOne(sql: String, vararg params: Any?, action: RowMapper<T>): T? =
    selectFirst(sql, *params, action = action)

@Deprecated(
    "Renamed to selectFirst",
    ReplaceWith("selectFirst(sql, *params, action = action)"),
    DeprecationLevel.ERROR,
)
fun <T> DataSource.selectOne(sql: String, vararg params: Any?, action: RowMapper<T>): T? =
    selectFirst(sql, *params, action = action)

@Deprecated(
    "Renamed to TqRowSequence, which is AutoCloseable and single-pass",
    ReplaceWith("TqRowSequence"),
    DeprecationLevel.ERROR,
)
typealias ResultSetSequence<T> = TqRowSequence<T>
