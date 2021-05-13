# tekniq-core

A suite of tools that have no dependencies on other libraries making it clean and easy to use without any bloat. It
provides features such as

## **TqCache**

The native implementation has been deprecated in favor of the Caffeine based implementation. Recommend
reading [Caffeine](https://github.com/ben-manes/caffeine) for more details on the underlying implementation.

Please reference [tekniq-cache](https://github.com/sepatel/tekniq/tree/master/tekniq-cache) for more information about
an enhanced loading cache implementation.

## TqConfig

Yet another configuration concept but this one can also provide transformations of data for both basic and complex
object types as well in many cases. Also provides a way to merge multiple config sources
(such as with global property settings with local overrides) into a single interface.

Currently supports Environment, Properties, and backing map configs with other modules likely to support database backed
configurations and more.

```kotlin
// Vanilla properties backed configuration
val config = TqPropertiesConfig("/etc/myapp/config.properties")
```

But a more interesting use case for more advanced applications would be to create an application configuration object
with tiered checking.

```kotlin
object MyAppConfig : TqChainConfig(
    TqPropertiesConfig("./config.properties", stopOnFailure = false),
    TqPropertiesConfig("${System.getenv("HOME")}/config.properties", stopOnFailure = false),
    TqPropertiesConfig("/etc/myapp/config.properties", stopOnFailure = false),
    TqPropertiesConfig("classpath:/config.properties")
)

// later on you can access it like
fun test() {
    val port = MyAppConfig.getInt("port")
    val portWithDefault = MyAppConfig.getInt("port", 8080)
}
```

## TqTracking

A Tracking Carrier detection utility which supports determination of a string as being a tracking number for USPS, UPS,
FEDex, or other type of carriers. Also provides a link to the carriers website to pull up full details on the tracking
information as well.

```kotlin
val fedex = TqTracking.getTrackingType("999999999999")
println(fexex) // prints FedEx
val ups = TqTracking.getTrackingType("1Z9999W99999999999")
println(ups) // prints UPS
val fake = TqTracking.getTrackingType("9")
println(fake) // prints null
```

