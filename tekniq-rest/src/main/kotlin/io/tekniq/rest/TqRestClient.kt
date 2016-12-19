package io.tekniq.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

open class TqRestClient(val logHandler: RestLogHandler = NoOpRestLogHandler) {
    open fun delete(url: String, headers: Map<String, Any> = emptyMap()): TqResponse {
        return request("DELETE", url, headers = headers)
    }

    open fun get(url: String, headers: Map<String, Any> = emptyMap()): TqResponse {
        return request("GET", url, headers = headers)
    }

    open fun put(url: String, json: Any?, headers: Map<String, Any> = emptyMap()): TqResponse {
        return request("PUT", url, json, headers)
    }

    open fun post(url: String, json: Any?, headers: Map<String, Any> = emptyMap()): TqResponse {
        return request("POST", url, json, headers)
    }

    open fun transform(json: Any?) = when (json) {
        is String, is Number, is Boolean -> toString()
        else -> mapper.writeValueAsString(json)
    }

    protected fun request(method: String, url: String, json: Any? = null, headers: Map<String, Any> = emptyMap()): TqResponse {
        val payload: String = transform(json)
        var response: TqResponse? = null
        val duration = measureTimeMillis {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Content-Type", "application/json")
                headers.forEach {
                    setRequestProperty(it.key, it.value.toString())
                }
                if (json != null) {
                    setRequestProperty("Content-Length", payload.length.toString())
                    doOutput = true
                    outputStream.write(payload.toByteArray())
                }
            }

            try {
                val responseCode = conn.responseCode
                val stream = conn.errorStream ?: conn.inputStream
                response = TqResponse(responseCode, stream.bufferedReader().use { it.readText() }, conn.headerFields)
            } catch (e: IOException) {
                response = TqResponse(-1, e.message ?: "", conn.headerFields)
            }
        }
        logHandler.onRestLog(RestLog(method, url, duration = duration, request = payload, status = response!!.status, response = response!!.body))
        return response ?: TqResponse(-1, "", emptyMap())
    }
}

data class TqResponse(val status: Int, val body: String, private val headers: Map<String, Any>) {
    fun header(key: String): Any? {
        val value = headers[key] ?: return null
        if (value is Collection<*> && value.size == 1) {
            return value.first()
        }
        return value
    }

    fun headers(): Map<String, Any> {
        return headers.mapValues {
            if (it is Array<*> && it.size == 1) {
                return@mapValues it.get(0)!!
            }
            it
        }
    }

    inline fun <reified T : Any> jsonAs(): T = jsonAs(T::class)
    fun <T : Any> jsonAs(type: KClass<T>): T = mapper.readValue(body, type.java)
}

data class RestLog(val method: String, val url: String, val ts: Date = Date(), val duration: Long = 0, val request: String? = null, val status: Int = 0, val response: String? = null)

interface RestLogHandler {
    fun onRestLog(log: RestLog)
}

private object NoOpRestLogHandler : RestLogHandler {
    override fun onRestLog(log: RestLog) {
    }
}

private val mapper = ObjectMapper().registerModule(KotlinModule())
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

