package io.tekniq.config

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Process-wide default ObjectMapper used by [bindJackson] when no mapper is supplied explicitly.
 *
 * The out-of-the-box default is the standard tekniq mapper ([tqObjectMapper]): a Kotlin-aware mapper that
 * reads unknown enum values as null, accepts single values as arrays, and does not fail on unknown
 * properties. This is simply the cleanest default for the most common use across the tekniq universe (the
 * REST client uses the same configuration), not a REST-specific mapper. Configure once at application
 * bootstrap with [configure]; every subsequent [bindJackson] call without an explicit mapper will use the
 * configured instance. Configuration is one-shot per [configure] call — each call starts from a fresh
 * [tqObjectMapper] and applies the customizer, so re-calling it is safe and replaces the previous default.
 *
 * Example:
 * ```
 * TqJacksonConfig.configure {
 *     registerModule(JavaTimeModule())
 *     disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
 * }
 * ```
 */
object TqJacksonConfig {
    @Volatile
    private var instance: ObjectMapper = tqObjectMapper()

    /**
     * The current default mapper. Returns whatever was last set via [configure], or the default
     * [tqObjectMapper] if never configured. Read-only — call [configure] to replace.
     */
    val defaultMapper: ObjectMapper get() = instance

    /**
     * Replace the default mapper with `customizer(tqObjectMapper())`. Each call rebuilds the mapper from
     * the tekniq baseline, so previous customizations are discarded.
     */
    fun configure(customizer: ObjectMapper.() -> ObjectMapper) {
        instance = customizer(tqObjectMapper())
    }

    /**
     * Replace the default mapper with the supplied instance. Use this when you already have an
     * `ObjectMapper` from another source (e.g. a Spring bean, a DI container).
     */
    fun configure(mapper: ObjectMapper) {
        instance = mapper
    }

    /** Reset to the default [tqObjectMapper]. Primarily for tests. */
    fun reset() {
        instance = tqObjectMapper()
    }
}
