# tekniq-sparklin
A Kotlin HTTP Framework built on top of Spark with a DSL as easy to use
as NodeJS's Express or Ruby's Sinatra. Also provides a nice validation
framework, easy to use authorization management, auto-transformation of
body to complex object types, and more.

## Simple Hello World
```kotlin
Sparklin {
    get("*") { req, resp -> "Hello World" }
}
```

## Simple JSON example
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

## Integrated Validation Framework
```kotlin
Sparklin {
    post("/hello", { req, res ->
        required("name").string("name").date("birth").stopOnRejections()
        val mock = req.jsonAs<MockRequest>()
        mapOf("Input" to mock, "Output" to "It worked")
    })
}
```

## Exception Handling
```kotlin
Sparklin {
    exception(Exception::class, { e, req, res ->
        println("Serious exception happened here: ${e.message}")
        e.printStackTrace()
        Pair(500, listOf(ErrorBean("fubar", e.message), ErrorBean("snafu", "Just another for fun")))
    })

    get("/bad-auth", { req, res ->
        res.status(401)
        res.body("Beware of dogs")
        throw RuntimeException("Mommy please save me")
    })
}
```

## Authorization Management
The built-in validation framework makes it easy to determine what authz
a user has and to lock down the web services appropriately based on the
permissions. Below is a basic implementation to help convey the concept.

```kotlin
object SimpleAuthorizationManager : AuthorizationManager {
    override fun getAuthz(request: Request): Collection<String> {
        // logic to determine what authz a user has
        return listOf("IAMME")
    }
}

Sparklin(SparklinConfig(authorizationManager = SimpleAuthorizationManager)) {
    get("/hello", { req, res ->
        // Must have either ANONYMOUS or MOMMY permission
        authz("ANONYMOUS", "MOMMY").stopOnRejections()
        MockResponse("John")
    }
    
    get("/hello/:name", { req, res ->
        // Must have both ANONYMOUS and MOMMY permissions
        authz(all = true, "ANONYMOUS", "MOMMY").stopOnRejections()
        MockResponse(req.params("name"))
    }
    
}
```

## Static Files
Not all content served will be dynamic. Anything that is not found in a
static resource will be sought after via a route mapping before serving
up a 404 page.

```kotlin
val sparklinConfig = SparklinConfig(
        staticFiles = SparklinStaticFiles(externalFileLocation = "src/main/resources/ui")
)

Sparklin(sparklinConfig) {
    ...
}
```

## Modularity
Non-trivial applications posses many web service urls and are often
broken into multiple categories as well. It only makes sense to have
your code broken into multiple files as well to help maintain the code.

```kotlin
fun main(args: Array<String>) {
    val config = TqEnvConfig()
    val staticFiles: SparklinStaticFiles = if (config.get<String>("DEBUG") == "1") {
        println("DEBUG mode detected. Live reloading of UI resources")
        SparklinStaticFiles(externalFileLocation = "src/main/resources/ui")
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

fun handleExceptions(route: SparklinRoute) = route.apply {
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

fun routeLookupServices(route: SparklinRoute) = route.apply {
    get("/lookup/names") { req, resp -> LookupDao.names.values }
    get("/lookup/names/:id") { req, resp -> LookupDao.names[req.params("id")] }
    get("/lookup/orders") { req, resp -> LookupDao.orders.values }
    get("/lookup/orders/:id") { req, resp -> LookupDao.orders[req.params("id").toInt()] }
}
```
