# tekniq-core

A suite of tools that have no dependencies on other libraries making it clean and easy to use without any bloat. It
provides features such as

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
    val port = MyAppConfig.getInt("port") ?: 8080
}
```

## TqCron

A quick and easy way to handle cron calculations.

```kotlin
val cron = TqCron("3 7 * * * *") // Every hour at the 7th minute and 3 second marker
val nextRun = cron.next() // next trigger date
val relativeNextRun = cron.next(Date(0)) // first time it runs relative to the given date
```

## TqCryptography

Public/Private Key Encryption/Decryption/Signing/Verification utilities that are lightweight, fast, and compliant with
security audits and inspections. Also other quick and easy utilities for md5/sha256, base64 encoding/decoding, and more.

```kotlin
val key = TqCryptography.generateKeyPair()
val encrypted = key.encrypt("This is an encrypted message")
val decrypted = key.decrypt(encrypted)
assertTrue("This is an encrypted message", encrypted)

val hash = TqCryptography.sha2("I am a flying purple monkey")
assertEquals("36e590219098e573561b3cd3f703193f94f5d3f5e8f2cbc3f75468e06b6ba132", hash)

val hash = TqCryptography.md5("I am a flying purple monkey")
assertEquals("e1a3401853a457a79917b7a59e975333", hash)
```

## TqGlob

A quick and easy way to handle glob to regex conversions.

```kotlin
val regex = TqGlob.toRegEx("/user/*/edit")
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
