package io.tekniq.rest

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream
import kotlin.reflect.KClass

data class TqResponse(
    val status: Int,
    val stream: InputStream,
    private val headers: Map<String, Any>,
    private val mapper: ObjectMapper
) {
    val body: String by lazy { String(stream.readAllBytes()) }

    fun header(key: String): Any? {
        val value = headers[key] ?: return null
        if (value is Collection<*> && value.size == 1) {
            return value.first()
        }
        return value
    }

    fun headers(): Map<String, Any> {
        return headers.mapValues {
            val value = it.value
            if (value is Array<*> && value.size == 1) {
                return@mapValues value[0]!!
            }
            it
        }
    }

    inline fun <reified T : Any> jsonAs(): T = jsonAsNullable(T::class)!!
    inline fun <reified T : Any> jsonAsNullable(): T? = jsonAsNullable(T::class)
    fun <T : Any> jsonAsNullable(type: KClass<T>): T? = mapper.readValue(body, type.java)

    inline fun <reified T : Any> jsonArrayOf(): Iterator<T> = jsonArrayOf(T::class)
    fun <T : Any> jsonArrayOf(type: KClass<T>): Iterator<T> {
        val parser = mapper.factory.createParser(stream)
        if (parser.nextToken() != JsonToken.START_ARRAY) error("Invalid start of array")
        val it = object : Iterator<T> {
            var checked: Boolean? = null
            override fun hasNext(): Boolean {
                val next = checked
                if (next != null) return next
                return (parser.nextToken() != JsonToken.END_ARRAY).also { checked = it }
            }

            override fun next(): T {
                if (hasNext()) return mapper.readValue(parser, type.java).also { checked = null }
                throw NoSuchElementException()
            }
        }
        return it
    }
}
