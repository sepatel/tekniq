# tekniq-cache

Kotlin's idiom version of the Caffeine library (high performance cache library), this loading cache supports
concurrency, dynamic loading of content, access/write expiration, and more.

```kotlin
// Trivial example
val square = TqLoadingCache<Int, Int> { it * it }
```

```kotlin
// Example with all options utilized
// Will purge after 30 seconds of non-access, 60 seconds from load,
// auto-refresh itself 45 seconds after initial load, max 10 records
// stored in cache, and will record the hit/misses on the cache.
data class Person(val name: String, val age: Int)

val people = TqLoadingCache<Int, Person>(
    expireAfterAccess = 30000,
    expireAfterWrite = 60000,
    refreshAfterWrite = 45000,
    maximumSize = 10,
    recordStats = true
) {
    conn.selectOne("""SELECT name, age FROM person WHERE id=?""", it) {
        Person(getString("name"), getInt("age"))
    }
}
```

Recommend reading [Caffeine](https://github.com/ben-manes/caffeine) for more details on the underlying implementation.
