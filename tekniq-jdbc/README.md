# tekniq-jdbc
Provides extensions to the DataSource and Connection objects allowing
one to more cleanly and easily work with the JDBC APIs with the kotlin
idiom supported. Does not require overhead of object mappings or such.

Also provided is a single connection datasource as a convenience but it
is highly recommend that something like vibur-dbcp be used instead for
your actual datasource needs. The extensions provided by this library
will cause all datasources and connection instances to be used in the
kotlin idiom as described below.

## Select One
Returns the first row it finds or null if no rows matched

```kotlin
// datasource will obtain connection, execute query, and release connection
val person = ds.selectOne("SELECT name, age FROM person WHERE id=?", 42) {
  Person(getString("name"), getInt("age"))
}

// connection will execute query only
val person = conn.selectOne("SELECT name, age FROM person WHERE id=?", 42) {
  Person(getString("name"), getInt("age"))
}
```

## Select
Can either act upon or return a list of transformed results found

```kotlin
// datasource will obtain connection, execute query, and release connection
val people = ds.select("SELECT name, age FROM person") {
  Person(getString("name"), getInt("age"))
}

// connection will execute query only
val person = conn.select("SELECT name, age FROM person") {
  Person(getString("name"), getInt("age"))
}

// select without returning a list also available on connection level
// not building objects or a list to be returned
ds.select("SELECT name, age FROM person") {
  log("${getString("name")} is ${getint("age")} years old")
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
val star = conn.call("{CALL foo.my_custom_pkg.method_name(?, ?, ?)}") {
  setString("p_name", "John")
  setAge("p_age", 42)
  registerOutParameter("x_star_sign", Types.VARCHAR)
  execute()
  getString("x_star_sign")
}
println("Executed complex method to determine star sign of $star")
```

## Transaction
Create a transaction space which will auto-rollback if any exception is
thrown. Must be explicitly committed at the end or will be rolled back.

```kotlin
// only available on the datasource extension
// will obtain a connection, set auto-commit to false, and configure the
// desired transaction level defaulting to read committed
ds.transaction {
  conn.insert("INSERT INTO person(name, age) VALUES(?, ?)", "John", 20)
  // rollback()
  conn.update("UPDATE person SET age=age * 2 WHERE age < ?", 20)
  conn.delete("DELETE FROM person WHERE age < ?", 20)

  val person = ds.selectOne("SELECT name, age FROM person WHERE id=?", 42) {
    Person(getString("name"), getInt("age"))
  }
  
  call("{CALL foo.my_custom_pkg.method_name(?, ?, ?)}") {
    setString("p_name", "John")
    setAge("p_age", 42)
    registerOutParameter("x_star_sign", Types.VARCHAR)
    execute()
    val star = getString("x_star_sign")
    println("Executed complex method to determine star sign of $star")
  }
  commit() // will automatically rollback if not explicitly committed
}
```
