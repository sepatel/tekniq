# tekniq-jdbc

Provides extensions to the DataSource and Connection objects allowing one to more cleanly and easily work with the JDBC
APIs with the kotlin idiom supported. Does not require overhead of object mappings or such.

Also provided is a single connection datasource as a convenience but it is highly recommend that something like
vibur-dbcp be used instead for your actual datasource needs. The extensions provided by this library will cause all
datasources and connection instances to be used in the kotlin idiom as described below.

## Select First

Returns the first row it finds or null if no rows matched. The mapper runs with the `ResultSet` as
its receiver, so columns are read directly rather than through an `it` parameter.

```kotlin
// datasource will obtain connection, execute query, and release connection
val person = ds.selectFirst("SELECT name, age FROM person WHERE id=?", 42) {
  Person(getString("name"), getInt("age"))
}

// connection will execute query only
val person = conn.selectFirst("SELECT name, age FROM person WHERE id=?", 42) {
  Person(getString("name"), getInt("age"))
}
```

## Select

Reads every row into a list, releasing the statement and cursor before returning.

```kotlin
// datasource will obtain connection, execute query, and release connection
val people = ds.select("SELECT name, age FROM person") {
  Person(getString("name"), getInt("age"))
}

// connection will execute query
val people = conn.select("SELECT name, age FROM person") {
  Person(getString("name"), getInt("age"))
}

// for side effects, without building objects to return
conn.select("SELECT name, age FROM person") {
  log("${getString("name")} is ${getInt("age")} years old")
}
```

## Stream

For result sets too large to hold in memory. The sequence owns the statement and cursor, so drain it
or wrap it in `use {}`. It reads a forward-only cursor, so it can only be iterated once.

```kotlin
conn.stream("SELECT name, age FROM person") { Person(getString("name"), getInt("age")) }
  .use { people -> people.first { it.age > 40 } }
```

There is no `DataSource.stream`: the connection would have to be released before the first row was
read, handing a live cursor back to the pool. Stream from a connection you hold instead.

## Named Parameters

Pass a single map to bind `:name` placeholders. Quoted literals, comments and `::` casts are left
alone, and a placeholder with no matching entry throws rather than binding null.

```kotlin
val person = conn.selectFirst("SELECT name FROM person WHERE id = :id", mapOf("id" to 42)) {
  getString("name")
}
```

## Update/Delete/Insert

```kotlin
// same as with datasource extension
val rows = conn.update("UPDATE person SET age=age * 2 WHERE age < ?", 20)
val rows = conn.delete("DELETE FROM person WHERE age < ?", 20)
val rows = conn.insert("INSERT INTO person(name, age) VALUES(?, ?)", "John", 20)
```

## Callable

Can either return a transformed value or act within the localized space

```kotlin
// same as with datasource extension
val conn = ds.connection

// Unit returned
conn.call("{CALL foo.my_custom_pkg.method_name(?, ?, ?)}") {
  setString("p_name", "John")
  setAge("p_age", 42)
  registerOutParameter("x_star_sign", Types.VARCHAR)
  execute()
  val star = getString("x_star_sign")
  println("Executed complex method to determine star sign of $star")
}

// String returned
val star = conn.call<String>("{CALL foo.my_custom_pkg.method_name(?, ?, ?)}") {
  setString("p_name", "John")
  setAge("p_age", 42)
  registerOutParameter("x_star_sign", Types.VARCHAR)
  execute()
  getString("x_star_sign")
}
println("Executed complex method to determine star sign of $star")
```

## Transaction

Create a transaction space which will auto-rollback if any exception is thrown. Will be committed at the end unless
commitOnCompletion is set to false.

```kotlin
// only available on the datasource extension
// will obtain a connection, set auto-commit to false, and configure the
// desired transaction level defaulting to read committed
ds.transaction {
  // the block's receiver IS the transaction's connection -- call extensions on it directly
  insert("INSERT INTO person(name, age) VALUES(?, ?)", "John", 20)
  update("UPDATE person SET age=age * 2 WHERE age < ?", 20)
  delete("DELETE FROM person WHERE age < ?", 20)

  // NOT ds.selectFirst(...) -- that checks out a second connection and would not see the
  // uncommitted rows above
  val person = selectFirst("SELECT name, age FROM person WHERE id=?", 42) {
    Person(getString("name"), getInt("age"))
  }

  call("{CALL foo.my_custom_pkg.method_name(?, ?, ?)}") {
    it.setString("p_name", "John")
    it.registerOutParameter("x_star_sign", Types.VARCHAR)
    it.execute()
    println("Star sign is ${it.getString("x_star_sign")}")
  }

  person // the block's value is returned; commit happens on the way out
}
```

Pass `commitOnCompletion = false` to manage the transaction yourself. A commit that itself fails
rolls back, so a dirty connection is never returned to the pool.

Returning early with a non-local `return` skips the commit and discards the work — return a value
from the block instead.
