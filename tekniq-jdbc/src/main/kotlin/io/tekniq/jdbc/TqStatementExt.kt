@file:Suppress("TooManyFunctions")
package io.tekniq.jdbc

import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date

// Alternatives are ordered so that anything a colon may legitimately live inside is consumed before
// the placeholder rule gets a chance at it: quoted literals (with '' / "" escapes), line and block
// comments, and PostgreSQL's :: cast. Only the final alternative captures a group, so only it is
// ever rewritten -- 'a:b', --  :note and id::text all pass through byte for byte.
private val sqlToken = Regex(
    """'(?:[^']|'')*'|"(?:[^"]|"")*"|--[^\n]*|/\*.*?\*/|::\w+|:([A-Za-z_]\w*)""",
    RegexOption.DOT_MATCHES_ALL,
)

/** Rewrites `:name` placeholders to positional `?` and returns them in binding order. */
fun parseNamedParameters(sql: String): Pair<String, List<String>> {
    val names = mutableListOf<String>()
    val normalizedSql = sqlToken.replace(sql) { match ->
        val name = match.groups[1]?.value ?: return@replace match.value
        names += name
        "?"
    }
    return normalizedSql to names
}

/**
 * Resolves [sql] and [params] into the statement to prepare and the values to bind.
 *
 * Named-parameter rewriting only happens when the caller supplied exactly one [Map]; every other
 * call hands the SQL to the driver untouched, so a query can never be reshaped behind the caller's
 * back. A name with no corresponding map entry fails loudly instead of binding null.
 */
internal fun resolveStatement(sql: String, params: Array<out Any?>): Pair<String, List<Any?>> {
    @Suppress("UNCHECKED_CAST")
    val values = params.singleOrNull() as? Map<String, Any?> ?: return sql to params.asList()
    val (normalizedSql, names) = parseNamedParameters(sql)
    if (names.isEmpty()) return sql to params.asList()

    val missing = names.filterNot(values::containsKey).distinct()
    require(missing.isEmpty()) { "No value supplied for named parameter(s) $missing in: $sql" }
    return normalizedSql to names.map(values::get)
}

fun PreparedStatement.applyParams(vararg params: Any?) {
    params.forEachIndexed { index, param ->
        when (param) {
            is Time -> setTime(index + 1, param)
            is LocalTime -> setTime(index + 1, Time.valueOf(param))
            is java.sql.Date -> setDate(index + 1, param)
            is LocalDate -> setDate(index + 1, java.sql.Date.valueOf(param))
            is ZonedDateTime -> setTimestamp(index + 1, Timestamp.from(param.toInstant()))
            is LocalDateTime -> setTimestamp(index + 1, Timestamp.valueOf(param))
            is Date -> setTimestamp(index + 1, Timestamp(param.time))
            is Calendar -> setTimestamp(index + 1, Timestamp(param.timeInMillis))
            else -> setObject(index + 1, param)
        }
    }
}

fun PreparedStatement.setBooleanNull(index: Int, x: Boolean?) = if (x == null) setNull(index, Types.BOOLEAN) else setBoolean(index, x)
fun PreparedStatement.setByteNull(index: Int, x: Byte?) = if (x == null) setNull(index, Types.TINYINT) else setByte(index, x)
fun PreparedStatement.setShortNull(index: Int, x: Short?) = if (x == null) setNull(index, Types.SMALLINT) else setShort(index, x)
fun PreparedStatement.setIntNull(index: Int, x: Int?) = if (x == null) setNull(index, Types.INTEGER) else setInt(index, x)
fun PreparedStatement.setLongNull(index: Int, x: Long?) = if (x == null) setNull(index, Types.BIGINT) else setLong(index, x)
fun PreparedStatement.setFloatNull(index: Int, x: Float?) = if (x == null) setNull(index, Types.FLOAT) else setFloat(index, x)
fun PreparedStatement.setDoubleNull(index: Int, x: Double?) = if (x == null) setNull(index, Types.DOUBLE) else setDouble(index, x)

fun CallableStatement.setBooleanNull(paramName: String, x: Boolean?) = if (x == null) setNull(paramName, Types.BOOLEAN) else setBoolean(paramName, x)
fun CallableStatement.setByteNull(paramName: String, x: Byte?) = if (x == null) setNull(paramName, Types.TINYINT) else setByte(paramName, x)
fun CallableStatement.setShortNull(paramName: String, x: Short?) = if (x == null) setNull(paramName, Types.SMALLINT) else setShort(paramName, x)
fun CallableStatement.setIntNull(paramName: String, x: Int?) = if (x == null) setNull(paramName, Types.INTEGER) else setInt(paramName, x)
fun CallableStatement.setLongNull(paramName: String, x: Long?) = if (x == null) setNull(paramName, Types.BIGINT) else setLong(paramName, x)
fun CallableStatement.setFloatNull(paramName: String, x: Float?) = if (x == null) setNull(paramName, Types.FLOAT) else setFloat(paramName, x)
fun CallableStatement.setDoubleNull(paramName: String, x: Double?) = if (x == null) setNull(paramName, Types.DOUBLE) else setDouble(paramName, x)