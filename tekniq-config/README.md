# tekniq-config

Additional config sources and tooling on top of `tekniq-core`. Adds JSON and YAML file backings, runtime
reload/watching, and the Jackson-flavored type-safe bind.

## Type-Safe Config Binding (Jackson variant)

For config classes that use Jackson annotations, `bindJackson<T>()` delegates to `bind<T>()` and then
re-runs the result through `ObjectMapper.convertValue`, which applies annotations like `@JsonInclude`,
`@JsonIgnore`, `@JsonProperty`, and any custom serializers or modules registered on the supplied mapper.

```kotlin
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
data class MongoConfig(
    @JsonProperty("uri") val uri: String,
    @JsonProperty("database") val database: String,
    @JsonProperty("pool") val pool: MongoPoolConfig = MongoPoolConfig()
)

data class MongoPoolConfig(
    @JsonProperty("min") val min: Int = 0,
    @JsonProperty("max") val max: Int = 100
)

val config = TqJsonConfig("classpath:/mongo.json")
val mapper = jacksonObjectMapper()
val mongo = config.bindJackson<MongoConfig>(mapper, prefix = "mongo")
```

The same data class works with `bind<MongoConfig>()` for the no-deps path and `bindJackson<MongoConfig>()` for
the annotated path, so projects can share config DTOs across modules. Note that input-side Jackson features
like `@JsonAlias` are not supported — `bind` reads keys by Kotlin parameter name. Use plain Jackson
`ObjectMapper` if you need alias-based deserialization.

### Configuring the default mapper

When `bindJackson<T>()` is called without an explicit `ObjectMapper`, it uses the process-wide default
configured on `TqJacksonConfig`. Configure it once at application bootstrap and every subsequent
`bindJackson` call picks it up:

```kotlin
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.tekniq.config.TqJacksonConfig

TqJacksonConfig.configure {
    registerModule(JavaTimeModule())
    registerModule(ParameterNamesModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
```

There are two configuration entry points:

- `TqJacksonConfig.configure { ... }` — starts from a fresh `jacksonObjectMapper()` and applies the
  customizer block. Safe to call multiple times; each call replaces the previous default.
- `TqJacksonConfig.configure(mapper)` — supply a fully-built `ObjectMapper` (e.g. one from a DI container).
- `TqJacksonConfig.reset()` — restores the unmodified `jacksonObjectMapper()`. Useful in tests.

An explicit mapper passed to `bindJackson<T>(mapper, ...)` always wins over the configured default, so
specific call sites can opt out of the singleton when needed.
