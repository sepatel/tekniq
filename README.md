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

## tekniq-rest
A tool utilizing jackson-mapper for making JSON based RESTful calls to
web services.

```kotlin
val rest = TqRestClient()

// Simple GET
val resp = rest.get("https://www.google.com/")
println("Status: ${resp.status}, Body: ${resp.body.substring(0, 50)} ...")

// Data transformation
data class Fiddle(name: String, height: Int?, birth: Date?)
val resp = rest.get("https://example.com/myCustomWebService/json")
val x = resp.jsonAs<Fiddle>()
println(x)

// POST with Headers
val payload = Fiddle("John", 42, Date())
val resp = rest.post("https://example.com/myCustomWebService/json", payload, mapOf(
    "Authentication" to "Basic ${Base64.getEncoder().encode("user:pass".toByteArray())}"
))
println("Status: ${resp.status}, Body: ${resp.body")
val x = resp.jsonAs<Fiddle>()

// Logging Request and Response
val logrest = TqRestClient(object : RestLogHandler {
    override fun onRestLog(log: RestLog) {
        println("LOG Request: ${log.request}")
        println("LOG Response: ${log.response}")
     }
})
```


## tekniq-sparklin
A Kotlin HTTP Framework built on top of Spark with a DSL as easy to use
as NodeJS's Express or Ruby's Sinatra. Also provides a nice validation
framework, easy to use authorization management, auto-transformation of
body to complex object types, and more.

*Simple Hello World*
```kotlin
Sparklin {
    get("*") { req, resp -> "Hello World" }
}
```

*Simple JSON example*
```kotlin
private data class MockRequest(val name: String, val age: Int, val created: Date? = Date())
private data class MockResponse(val color: String, val grade: Int = 42, val found: Date? = Date(), val nullable: String? = null)
Sparklin(SparklinConfig(port = 9999)) {
    before { req, res -> res.header("Content-type", "application/json") }

    get("/test") { req, res -> MockResponse("purple", found = Date(4200)) }
    post("/spitback") { req, res ->
        val mock = req.jsonAs<MockRequest>() ?: return@post null
        MockResponse(mock.name, mock.age, mock.created)
    }
}
```

*Validation framework example*
```kotlin
Sparklin {
    post("/hello", { req, res ->
        required("name").string("name").date("birth").stopOnRejections()
        val mock = req.jsonAs<MockRequest>()
        mapOf("Input" to mock, "Output" to "It worked")
    })
}
```

*Exception Handling example*
```kotlin
Sparklin {
    exception(Exception::class, { e, req, res ->
        println("Serious exception happened here: ${e.message}")
        e.printStackTrace()
        Pair(500, listOf(ErrorBean("fubar", e.message), ErrorBean("snafu", "Just another for fun")))
    })

    get("/test", { req, res ->
        res.status(401)
        res.body("Fiddle")
        throw NullPointerException("Mommy please save me")
    })
}
```

*Authentication example*
```kotlin
object SimpleAuthorizationManager : AuthorizationManager {
    override fun getAuthz(request: Request): Collection<String> {
        return listOf("IAMME")
    }
}

Sparklin(SparklinConfig(authorizationManager = SimpleAuthorizationManager)) {
    get("/hello", { req, res ->
        hasAny("ANONYMOUS", "MOMMY").stopOnRejections()
        MockResponse("John")
    }
    
    get("/hello/:name", { req, res ->
        hasAny("ANONYMOUS", "MOMMY").stopOnRejections()
        MockResponse(req.params("name"))
    }
    
}
```

*Static Files example*
```kotlin
val sparklinConfig = SparklinConfig(
        staticFiles = SparklinStaticFiles(externalFileLocation = "webapp/src/main/resources/ui")
)

Sparklin(sparklinConfig) {
    ...
}
```

*Modularity example*
```kotlin
fun main(args: Array<String>) {
    val config = TqEnvConfig()
    val staticFiles: SparklinStaticFiles = if (config.get<String>("DEBUG") == "1") {
        println("DEBUG mode detected. Live reloading of UI resources")
        SparklinStaticFiles(externalFileLocation = "webapp/src/main/resources/ui")
    } else {
        SparklinStaticFiles(fileLocation = "/ui")
    }

    val sparklinConfig = SparklinConfig(
            authorizationManager = ActiveDirectionAuthorizationManager,
            staticFiles = staticFiles
    )
    Sparklin(sparklinConfig) {
        after { req, res -> res.type("application/json") }

        handleExceptions(this)
        routeLookupServices(this)

        get("*") { req, resp ->
            throw NotFoundResource(req.requestMethod(), req.pathInfo(), req.params())
        }
    }.apply {
        println("Application started on ${sparklinConfig.ip}:${sparklinConfig.port}")
    }
}

class NotFoundResource(val method: String, val path: String, val params: Map<String, String> = emptyMap()) : Exception() {
}

fun handleExceptions(route: SparklinRoute) {
    route.apply {
        exception(NotFoundResource::class) { e, req, res ->
            Pair(404, mapOf("errors" to listOf<Rejection>(
                    Rejection("notFound", "${req.requestMethod()} ${req.pathInfo()}")
            )))
        }

        exception(Exception::class) { e, req, res ->
            logger.error(e.message, e)
            Pair(500, e.message ?: "Unexpected error $e")
        }
    }
}

fun routeLookupServices(route: SparklinRoute) {
    route.apply {
        get("/lookup/names") { req, resp -> LookupDao.names.values }
        get("/lookup/names/:id") { req, resp -> LookupDao.names[req.params("id")] }
        get("/lookup/orders") { req, resp -> LookupDao.paymentTerms.values }
        get("/lookup/orders/:id") { req, resp -> LookupDao.paymentTerms[req.params("id").toInt()] }
    }
}
```
