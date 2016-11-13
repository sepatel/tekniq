# tekniq-core
A suite of tools that have no dependencies on other libraries making
it clean and easy to use without any bloat. It provides features such as


## **TqCache**
A loading cache that supports concurrency, dynamic loading of content,
access/write expiration, and more. It is written with the kotlin idiom
and is more powerful then Java 8's computeIfAbsent.

```kotlin
// Trivial example
val square = TqCache<Int, Int> { it * it }
```

```kotlin
// Example reading person object from database
data class Person(val name: String, val age: Int)
val people = TqCache<Int, Person>(expireAfterWrite = 3000) {
  conn.selectOne("""SELECT name, age FROM person WHERE id=?""", it) {
    Person(getString("name"), getInt("age"))
  }
}
```


## TqConfig
Yet another configuration concept but this one can also provide
transformations of data for both basic and complex object types as well
in many cases. Also provides a way to merge multiple config sources
(such as with global property settings with local overrides) into a
single interface.

Currently supports Environment, Properties, and backing map configs with
other modules likely to support database backed configurations and more.

```kotlin
// Vanilla properties backed configuration
val config = TqPropertiesConfig("/etc/myapp/config.properties")
```

But a more interesting use case for more advanced applications would be
to create an application configuration object with tiered checking.

```kotlin
object MyAppConfig : TqChainConfig(
    TqPropertiesConfig("./config.properties", stopOnFailure = false),
    TqPropertiesConfig("${System.getenv("HOME")}/config.properties", stopOnFailure = false),
    TqPropertiesConfig("/etc/myapp/config.properties", stopOnFailure = false),
    TqPropertiesConfig("classpath:/config.properties")
)

// later on anywhere you in your you can access it like
fun test() {
    val port = MyAppConfig.getInt("port")
    val portWithDefault = MyAppConfig.getInt("port", 8080)
}
```


## TqTracking
A Tracking Carrier detection utility which supports determination of a
string as being a tracking number for USPS, UPS, FEDex, or other type of
carriers. Also provides a link to the carriers website to pull up full
details on the tracking information as well.

```kotlin
val fedex = TqTracking.getTrackingType("999999999999")
println(fexex) // prints FedEx
val ups = TqTracking.getTrackingType("1Z9999W99999999999")
println(ups) // prints UPS
val fake = TqTracking.getTrackingType("9")
println(fake) // prints null
```

