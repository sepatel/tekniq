# Tekniq-JDBC Modernization - COMPLETED

## API Changes (Implemented)

| Method | Returns | Behavior |
|--------|---------|----------|
| `select(sql)` | `CachedRowSet` | Eager, disconnected |
| `select(sql, vararg params, action)` | `Sequence<T>` | Lazy - action returns T |
| `selectFirst(sql, vararg params, action)` | `T?` | Eager, first row only |

**Breaking changes:**
- `stream(sql, action)` → `select(sql, action)` (Sequence)
- `selectOne(sql, action)` → `selectFirst(sql, action)` (T?)
- `List<T>` returning `select` variants → **Removed** (use `.toList()` on Sequence)
- `Unit` returning `select` variants → **Removed**

## Features Implemented

1. **Named Parameters** - `:name` syntax
   ```kotlin
   select("SELECT * FROM users WHERE id = :id", "id" to 42) { ... }
   ```

2. **Type-Safe Row Mapping** - `rs["column"]` / `rs[1]` indexable access
   ```kotlin
   val name = rs["name"]
   val age = rs[2]
   ```

3. **Resource Leak Fix** - `ResultSetIterator` closes ResultSet when exhausted

4. **Sequence.use() Extension** - for proper resource cleanup
   ```kotlin
   select(sql) { ... }.use { seq -> seq.firstOrNull() }
   ```

5. **Refactor Duplication** - DataSource delegates to Connection extensions

6. **TypeAliases** - `RowMapper<T>`, `ParamMap`

## Files Updated

| File | Changes |
|------|---------|
| `TqConnectionExt.kt` | New API (select, selectFirst, selectCached), delegates to Connection |
| `TqDataSourceExt.kt` | Delegates to Connection extensions |
| `TqStatementExt.kt` | Named parameter support via `applyNamedParameters` |
| `TqResultSetExt.kt` | Added indexable `[]` operator + additional null getters |
| `ResultSetIterator.kt` | Minor cleanup |
| `ResultSequenceExt.kt` (new) | Added `Sequence.use()` extension |
| `TqSingleConnectionDataSource.kt` | No changes needed |

## Remaining Items

- [ ] Transaction improvements (constants for isolation levels, savepoint support)

## Usage Examples

```kotlin
// CachedRowSet (disconnected)
val cached = ds.select("SELECT * FROM users")

// Lazy sequence
val users = ds.select("SELECT * FROM users") { User(getInt("id"), getString("name")) }
users.filter { it.active }.forEach { ... }

// First row
val user = ds.selectFirst("SELECT * FROM users WHERE id = ?", 42) {
    User(getInt("id"), getString("name"))
}

// Named parameters
val user = ds.selectFirst("SELECT * FROM users WHERE id = :id", mapOf("id" to 42)) {
    User(getInt("id"), getString("name"))
}

// Type-safe column access
val name = rs["name"]
val age = rs[2]
```