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

### Type-Safe Config Binding

Stringly-typed `getInt("port")` lookups silently produce `null` on typos and force every call site to handle
nullability. `bind<T>()` replaces that with a typed, fail-fast read: a data class is bound at startup and any
missing or wrong-type value throws `TqConfigBindException` with the exact config path.

```kotlin
@TqConfigPrefix("mongo")
data class MongoConfig(
    val uri: String,
    val database: String,
    val pool: MongoPoolConfig = MongoPoolConfig(),
    val retry: MongoRetryConfig = MongoRetryConfig(),
    val ssl: MongoSslConfig? = null,
    val options: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList()
)

data class MongoPoolConfig(val min: Int = 0, val max: Int = 100, val acquireTimeoutMs: Long = 30_000)
data class MongoRetryConfig(val attempts: Int = 3, val backoffMs: Long = 100)
data class MongoSslConfig(val enabled: Boolean = false, val trustStorePath: String? = null)

object AppConfig : TqChainConfig(
    TqPropertiesConfig("/etc/myapp/mongo.properties"),
    TqPropertiesConfig("classpath:/mongo.properties")
)

val mongo = AppConfig.bind<MongoConfig>()
```

The same `bind<MongoConfig>()` reads from any `TqConfig` source (properties, JSON via `tekniq-config`, env,
chained). Defaults declared on the data class are honored, nested data classes are bound recursively, and `List`
plus `Map<String, V>` are resolved automatically. To pick a different prefix, pass it explicitly:
`config.bind<MongoConfig>(prefix = "ordersMongo")`. For the Jackson variant (sealed types, custom
deserializers, `@JsonInclude` post-processing) see `tekniq-config`.

## TqCron

A quick and easy way to handle cron calculations.

```kotlin
val cron = TqCron("3 7 * * * *") // Every hour at the 7th minute and 3 second marker
val nextRun = cron.next() // next trigger date
val relativeNextRun = cron.next(Date(0)) // first time it runs relative to the given date
```

## TqKeyPair

Asymmetric keys for RSA and Ed25519 behind one sealed type. Every key half stores its standard DER encoding as a
Base64 string — the same bytes a PEM file carries — so keys persist to any string column and rebuild from it.

```kotlin
val rsa = TqKeyPair.Rsa.generate()          // optional bits = 2048
val ed = TqKeyPair.Ed25519.generate()
```

Signing and verification are identical across algorithms, so code can accept a `TqKeyPair` and stay
algorithm-agnostic:

```kotlin
fun stamp(key: TqKeyPair, message: String): String = key.sign(message) // Base64 signature

val signature = ed.sign("Guardians of the Galaxy")
ed.verify("Guardians of the Galaxy", signature) // true
```

`verify` fails closed — a tampered message, a corrupt signature or a non-Base64 string all return `false` rather
than throwing. Verification only needs the public half, which is the half you distribute:

```kotlin
val trusted = TqPublicKey.Ed25519(publishedX509)
trusted.verify(message, signature)

// Interop with SSH, JWK and other tooling that publishes bare 32-byte Ed25519 keys
val fromRaw = TqPublicKey.Ed25519.ofRaw(rawBytes)
val backToRaw = fromRaw.raw()
```

Encryption exists only on `TqKeyPair.Rsa`, because Ed25519 has no encryption scheme — calling `encrypt` on an
Ed25519 key is a compile error, not a runtime surprise. RSA encryption is hybrid: a single-use AES-256-GCM data
key is wrapped with RSA-OAEP-SHA256, so payloads of any size work and tampering is detected.

```kotlin
val encrypted = rsa.encrypt("This is an encrypted message")
rsa.decrypt(encrypted) // "This is an encrypted message"
```

Private keys redact themselves from `toString()`, so logging a key or a pair never leaks key material.

## TqCryptography

Keyless primitives: symmetric AES-GCM, digests, HMAC and Base64.

```kotlin
val encrypted = TqCryptography.aesGcmEncrypt(plaintext, key) // nonce || ciphertext || tag
val plaintext = TqCryptography.aesGcmDecrypt(encrypted, key) // throws if altered

val hash = TqCryptography.sha256("I am a flying purple monkey")
assertEquals("36e590219098e573561b3cd3f703193f94f5d3f5e8f2cbc3f75468e06b6ba132", hash)

val hash = TqCryptography.md5("I am a flying purple monkey")
assertEquals("e1a3401853a457a79917b7a59e975333", hash)

val digest = TqCryptography.hmac("Simple Digest Message", "Guardians of the Galaxy")
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
