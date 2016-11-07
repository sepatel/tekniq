# tekniq
A framework designed around Kotlin. Modules include

## tekniq-core
A suite of tools that have no dependencies on any other libraries making
it clean and easy to use without any bloat. It provides such features as

* **Loading Cache** is a cache that supports concurrency, dynamic loading
of content, access/write expirations, and more. It is based around the
original oc4j and guava design concepts but with the kotlin idiom and
advantages of Java 8 technology behind it keeping it very powerful and
efficient.

```kotlin
// Trivial example
val square = TqCache<Int, Int> { it * it }

// Example reading person object from database
data class Person(val name: String, val age: Int)
val people = TqCache<Int, Person>(expireAfterWrite = 3000) {
  conn.selectOne("""SELECT name, age FROM person WHERE id=?""", it) {
    Person(getString("name"), getInt("age"))
  }
}
```

* **Tracking Carrier Detection** is some implementation logic that
supports determining if a string is a USPS, UPS, FEDex, or other type of
carriers or just a randomly made up string. And provides a link to the
carriers website to pull up full details on the tracking information as
well.
```kotlin
val fedex = TqTracking.getTrackingType("999999999999")
println(fexex) // prints FedEx
val ups = TqTracking.getTrackingType("1Z9999W99999999999")
println(ups) // prints UPS
val fake = TqTracking.getTrackingType("9")
println(fake) // prints null
```

* **Configuration** is yet another configuration concept but this one can
also provide transformations of data for both basic and complex object
types as well in many cases. Also provides a way to merge multiple
sources of configurations into a single interface. Such as merging the
Environmental settings with a database configuration table with a local
code based configuration into a single object which is passed around.

## tekniq-jdbc
Provides extensions to the DataSource and Connection objects allowing
one to more cleanly and easily work with the JDBC APIs with the kotlin
idiom supported. Does not require overhead of object mappings or such.

**Select One**
Returns the first row it finds or null if no rows matched
```kotlin
// datasource will obtain connection, execute query, and release connection
val ds = <however you obtain a datasource normally>
val person = ds.selectOne("SELECT name, age FROM person WHERE id=?", 42) {
  Person(getString("name"), getInt("age"))
}

// connection will execute query only
val conn = ds.connection
val person = conn.selectOne("SELECT name, age FROM person WHERE id=?", 42) {
  Person(getString("name"), getInt("age"))
}
```

**Select**
Can either act upon or return a list of transformed results found
```kotlin
// datasource will obtain connection, execute query, and release connection
val ds = <however you obtain a datasource normally>
val people = ds.select("SELECT name, age FROM person") {
  Person(getString("name"), getInt("age"))
}

// connection will execute query only
val conn = ds.connection
val person = conn.select("SELECT name, age FROM person") {
  Person(getString("name"), getInt("age"))
}

// select without returning a list also available on connection level
// not building objects or a list to be returned
ds.select("SELECT name, age FROM person") {
  log("${getString("name")} is ${getint("age")} years old")
}
```

**Update/Delete/Insert**
```kotlin
// same as with datasource extension
val conn = ds.connection
val rows = conn.update("UPDATE person SET age=age * 2 WHERE age < ?", 20)
val rows = conn.delete("DELETE FROM person WHERE age < ?", 20)
val rows = conn.insert("INSERT INTO person(name, age) VALUES(?, ?)", "John", 20)
```

**Callable**
Can either return a transformed value or act within the localized space
```kotlin
// same as with datasource extension
val conn = ds.connection

// nothing returned
conn.call("{CALL foo.my_custom_pkg.method_name(?, ?, ?)}") {
  setString("p_name", "John")
  setAge("p_age", 42)
  registerOutParameter("x_star_sign", Types.VARCHAR)
  execute()
  val star = getString("x_star_sign")
  println("Executed complex method to determine star sign of $star")
}

// returning value for use elsewhere
val star = conn.call("{CALL foo.my_custom_pkg.method_name(?, ?, ?)}") {
  setString("p_name", "John")
  setAge("p_age", 42)
  registerOutParameter("x_star_sign", Types.VARCHAR)
  execute()
  getString("x_star_sign")
}
println("Executed complex method to determine star sign of $star")
```

**Transaction**
Create a transaction space which will auto-rollback if any exception is
thrown. Must be explicitly committed at the end or will be rolled back.
```kotlin
// only available on the datasource extension
// will obtain a connection, set auto-commit to false, and configure the
// desired transaction level defaulting to read committed
ds.transaction {
  conn.insert("INSERT INTO person(name, age) VALUES(?, ?)", "John", 20)
  rollback()
  conn.update("UPDATE person SET age=age * 2 WHERE age < ?", 20)
  conn.delete("DELETE FROM person WHERE age < ?", 20)
  commit()

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
}
```

## tekniq-rest
A tool utilizing jackson-mapper for making JSON based RESTful calls to
web services. _(TODO: Place a small example)_

## tekniq-sparklin
A Kotlin HTTP Framework built on top of Spark with a DSL as easy to use
as NodeJS's Express or Ruby's Sinatra. Also provides a nice validation
framework, easy to use authorization management, auto-transformation of
body to complex object types, and more.

_(TODO: Pull in the sparklin project example before deprecating the
sparklin project)_
