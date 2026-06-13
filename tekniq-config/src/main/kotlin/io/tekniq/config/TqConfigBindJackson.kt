package io.tekniq.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlin.reflect.KClass

inline fun <reified T : Any> TqConfig.bindJackson(
    objectMapper: ObjectMapper = createRestObjectMapper(),
    prefix: String? = null
): T = bindJackson(T::class, objectMapper, prefix)

fun <T : Any> TqConfig.bindJackson(
    kClass: KClass<T>,
    objectMapper: ObjectMapper = createRestObjectMapper(),
    prefix: String? = null
): T {
    val bound = bind(kClass, prefix)
    return objectMapper.convertValue(bound, kClass.java)
}

fun createRestObjectMapper(): ObjectMapper {
    return ObjectMapper()
        .registerModule(
            com.fasterxml.jackson.module.kotlin.KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(com.fasterxml.jackson.module.kotlin.KotlinFeature.NullToEmptyCollection, false)
                .configure(com.fasterxml.jackson.module.kotlin.KotlinFeature.NullToEmptyMap, false)
                .configure(com.fasterxml.jackson.module.kotlin.KotlinFeature.NullIsSameAsDefault, false)
                .configure(com.fasterxml.jackson.module.kotlin.KotlinFeature.SingletonSupport, false)
                .configure(com.fasterxml.jackson.module.kotlin.KotlinFeature.StrictNullChecks, false)
                .build()
        )
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}
