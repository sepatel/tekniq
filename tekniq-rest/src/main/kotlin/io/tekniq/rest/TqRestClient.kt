package io.tekniq.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

@Suppress("unused")
open class TqRestClient(
    val logHandler: RestLogHandler = NoOpRestLogHandler,
    val mapper: ObjectMapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
    val allowSelfSigned: Boolean = false,
    @Deprecated("Define system property `-Djdk.internal.httpclient.disableHostnameVerification` instead")
    val ignoreHostnameVerifier: Boolean = false // ignored as it is a global alteration not isolated to this connection
) {
    private val ctx = SSLContext.getInstance("SSL").also {
        it.init(null, arrayOf(SelfSignedTrustManager), SecureRandom())
    }

    open fun delete(url: String, headers: Map<String, Any> = emptyMap(), timeoutInSec: Long = 30): TqResponse =
        request("DELETE", url, headers = headers, timeoutInSec = timeoutInSec)

    open fun get(url: String, headers: Map<String, Any> = emptyMap(), timeoutInSec: Long = 30): TqResponse =
        request("GET", url, headers = headers, timeoutInSec = timeoutInSec)

    open fun put(url: String, json: Any?, headers: Map<String, Any> = emptyMap(), timeoutInSec: Long = 30): TqResponse =
        request("PUT", url, json, headers, timeoutInSec = timeoutInSec)

    open fun post(
        url: String,
        json: Any?,
        headers: Map<String, Any> = emptyMap(),
        timeoutInSec: Long = 30
    ): TqResponse =
        request("POST", url, json, headers, timeoutInSec = timeoutInSec)

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

    open fun transform(json: Any?): String = when (json) {
        is String, is Number, is Boolean -> json.toString()
        else -> mapper.writeValueAsString(json)
    }

    protected fun request(
        method: String,
        url: String,
        json: Any? = null,
        headers: Map<String, Any> = emptyMap(),
        timeoutInSec: Long,
    ): TqResponse {
        val payload: String = transform(json)
        val response: TqResponse
        measureTimeMillis {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .let { if (allowSelfSigned) it.sslContext(ctx) else it }
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
            val request = HttpRequest.newBuilder(URI(url))
                .timeout(Duration.ofSeconds(timeoutInSec))
                .let {
                    var builder = it
                    headers.forEach { (k, v) -> builder = builder.header(k, v.toString()) }
                    builder
                }
                .let {
                    when (method) {
                        "GET" -> it.GET()
                        "PUT" -> it.PUT(BodyPublishers.ofString(transform(json)))
                        "POST" -> it.POST(BodyPublishers.ofString(transform(json)))
                        "DELETE" -> it.DELETE()
                        else -> it
                    }
                }
                .build()

            response = try {
                val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
                TqResponse(resp.statusCode(), resp.body(), resp.headers().map(), mapper)
            } catch (e: IOException) {
                TqResponse(-1, e.message ?: "", request.headers().map(), mapper)
            } catch (e: InterruptedException) {
                TqResponse(-1, e.message ?: "", request.headers().map(), mapper)
            }
        }.also {
            logHandler.onRestLog(
                RestLog(
                    method,
                    url,
                    duration = it,
                    request = payload,
                    status = response.status,
                    response = response.body
                )
            )
        }
        return response
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

fun interface RestLogHandler {
    fun onRestLog(log: RestLog)
}

private object NoOpRestLogHandler : RestLogHandler {
    override fun onRestLog(log: RestLog) {
    }
}

