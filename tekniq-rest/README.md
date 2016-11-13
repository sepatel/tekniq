# tekniq-rest
A tool utilizing jackson-mapper for making RESTful calls to web services.

## Simple GET
```kotlin
val rest = TqRestClient()
val resp = rest.get("https://www.google.com/")
println("Status: ${resp.status}, Body: ${resp.body.substring(0, 50)} ...")
```

## Data Transformations
```kotlin
data class Fiddle(name: String, height: Int?, birth: Date?)
val rest = TqRestClient()

val resp = rest.get("https://example.com/myCustomWebService/json")
val x = resp.jsonAs<Fiddle>()
println(x)
```

## POST with headers
```kotlin
data class Fiddle(name: String, height: Int?, birth: Date?)
val rest = TqRestClient()

val payload = Fiddle("John", 42, Date())
val resp = rest.post("https://example.com/myCustomWebService/json", payload, mapOf(
    "Authentication" to "Basic ${Base64.getEncoder().encode("user:pass".toByteArray())}"
))
println("Status: ${resp.status}, Body: ${resp.body")
val x = resp.jsonAs<Fiddle>()
```

## Logging Request and Response
This is useful for debugging or creating audit trails.

```kotlin
val logrest = TqRestClient(object : RestLogHandler {
    override fun onRestLog(log: RestLog) {
        println("LOG Request: ${log.request}")
        println("LOG Response: ${log.response}")
     }
})
```

