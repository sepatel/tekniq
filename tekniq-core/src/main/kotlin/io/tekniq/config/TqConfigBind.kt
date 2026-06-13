package io.tekniq.config

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TqConfigPrefix(val value: String)

inline fun <reified T : Any> TqConfig.bind(prefix: String? = null): T =
    bind(T::class, prefix)

fun <T : Any> TqConfig.bind(kClass: KClass<T>, prefix: String? = null): T {
    @Suppress("UNCHECKED_CAST")
    return TqConfigBinder(kClass, this).bind(prefix)
}
