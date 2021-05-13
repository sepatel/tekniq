package io.tekniq.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

@Suppress("unused")
open class TqRestClient(
    val logHandler: RestLogHandler = NoOpRestLogHandler,
    val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
    val allowSelfSigned: Boolean = false,
    val ignoreHostnameVerifier: Boolean = false
) {
    private val ctx = SSLContext.getInstance("SSL").apply {
        init(null, arrayOf(SelfSignedTrustManager), SecureRandom())
    }

    open fun delete(url: String, headers: Map<String, Any> = emptyMap()): TqResponse =
        request("DELETE", url, headers = headers)

    open fun get(url: String, headers: Map<String, Any> = emptyMap()): TqResponse =
        request("GET", url, headers = headers)

    open fun put(url: String, json: Any?, headers: Map<String, Any> = emptyMap()): TqResponse =
        request("PUT", url, json, headers)

    open fun post(url: String, json: Any?, headers: Map<String, Any> = emptyMap()): TqResponse =
        request("POST", url, json, headers)

    open fun <T : Any?> delete(url: String, headers: Map<String, Any> = emptyMap(), action: TqResponse.() -> T): T? {
        val response = delete(url, headers)
        return action(response)
    }

    open fun <T : Any?> get(url: String, headers: Map<String, Any> = emptyMap(), action: TqResponse.() -> T): T? {
        val response = get(url, headers)
        return action(response)
    }

    open fun <T : Any?> put(
        url: String,
        json: Any?,
        headers: Map<String, Any> = emptyMap(),
        action: TqResponse.() -> T
    ): T? {
        val response = put(url, json, headers)
        return action(response)
    }

    open fun <T : Any?> post(
        url: String,
        json: Any?,
        headers: Map<String, Any> = emptyMap(),
        action: TqResponse.() -> T
    ): T? {
        val response = post(url, json, headers)
        return action(response)
    }

    open fun transform(json: Any?) = when (json) {
        is String, is Number, is Boolean -> json.toString()
        else -> mapper.writeValueAsString(json)
    }

    protected fun request(
        method: String,
        url: String,
        json: Any? = null,
        headers: Map<String, Any> = emptyMap()
    ): TqResponse {
        val payload: String = transform(json)
        var response: TqResponse? = null
        val duration = measureTimeMillis {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                if (this is HttpsURLConnection) {
                    if (allowSelfSigned) {
                        sslSocketFactory = ctx.socketFactory
                    }
                    if (ignoreHostnameVerifier) {
                        hostnameVerifier = HostnameVerifier { _, _ -> true }
                    }
                }
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

            response = try {
                val responseCode = conn.responseCode
                val stream = conn.errorStream ?: conn.inputStream
                TqResponse(responseCode, stream.bufferedReader().use { it.readText() }, conn.headerFields, mapper)
            } catch (e: IOException) {
                TqResponse(-1, e.message ?: "", conn.headerFields, mapper)
            }
        }
        logHandler.onRestLog(
            RestLog(
                method,
                url,
                duration = duration,
                request = payload,
                status = response!!.status,
                response = response!!.body
            )
        )
        return response ?: TqResponse(-1, "", emptyMap(), mapper)
    }

    private object SelfSignedTrustManager : X509TrustManager {
        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
        override fun getAcceptedIssuers(): Array<out X509Certificate> = emptyArray()
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
    }
}

data class TqResponse(
    val status: Int,
    val body: String,
    private val headers: Map<String, Any>,
    private val mapper: ObjectMapper
) {
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
                return@mapValues it[0]!!
            }
            it
        }
    }

    inline fun <reified T : Any> jsonAs(): T = jsonAsNullable(T::class)!!
    inline fun <reified T : Any> jsonAsNullable(): T? = jsonAsNullable(T::class)
    fun <T : Any> jsonAsNullable(type: KClass<T>): T? = mapper.readValue(body, type.java)
}

data class RestLog(
    val method: String,
    val url: String,
    val ts: Date = Date(),
    val duration: Long = 0,
    val request: String? = null,
    val status: Int = 0,
    val response: String? = null
)

interface RestLogHandler {
    fun onRestLog(log: RestLog)
}

private object NoOpRestLogHandler : RestLogHandler {
    override fun onRestLog(log: RestLog) {
    }
}

