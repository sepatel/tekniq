package io.tekniq.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Process-wide default ObjectMapper used by [bindJackson] when no mapper is supplied explicitly.
 *
 * Configure once at application bootstrap with [configure]; every subsequent [bindJackson] call without
 * an explicit mapper will use the configured instance. Configuration is one-shot per [configure] call —
 * each call starts from a fresh `jacksonObjectMapper()` and applies the customizer, so re-calling it
 * is safe and replaces the previous default.
 *
 * Example:
 * ```
 * TqJacksonConfig.configure {
 *     registerModule(JavaTimeModule())
 *     registerModule(KotlinModule.Builder().build())
 *     disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
 * }
 * ```
 */
object TqJacksonConfig {
    @Volatile
    private var instance: ObjectMapper = jacksonObjectMapper()

    /**
     * The current default mapper. Returns whatever was last set via [configure], or the Kotlin
     * `jacksonObjectMapper()` if never configured. Read-only — call [configure] to replace.
     */
    val defaultMapper: ObjectMapper get() = instance

    /**
     * Replace the default mapper with `customizer(jacksonObjectMapper())`. Each call rebuilds the
     * mapper from scratch, so previous customizations are discarded.
     */
    fun configure(customizer: ObjectMapper.() -> ObjectMapper) {
        instance = customizer(jacksonObjectMapper())
    }

    /**
     * Replace the default mapper with the supplied instance. Use this when you already have an
     * `ObjectMapper` from another source (e.g. a Spring bean, a DI container).
     */
    fun configure(mapper: ObjectMapper) {
        instance = mapper
    }

    /** Reset to the unmodified `jacksonObjectMapper()`. Primarily for tests. */
    fun reset() {
        instance = jacksonObjectMapper()
    }
}
