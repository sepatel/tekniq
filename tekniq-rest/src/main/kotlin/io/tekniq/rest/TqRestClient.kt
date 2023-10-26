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
import java.net.http.WebSocket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Suppress("unused")
open class TqRestClient(
    val mapper: ObjectMapper = ObjectMapper()
        .registerModule(
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
    val ignoreHostnameVerifier: Boolean = false, // ignored as it is a global alteration not isolated to this connection
    connectTimeout: Duration = Duration.ofSeconds(10),
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .let { if (allowSelfSigned) it.sslContext(ctx) else it }
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val ctx = SSLContext.getInstance("SSL").also {
        it.init(null, arrayOf(SelfSignedTrustManager), SecureRandom())
    }

    open fun openWebSocket(
        url: String,
        headers: Map<String, String> = emptyMap(),
        connectTimeout: Duration = Duration.ofSeconds(10),
        subprotocols: Array<String> = emptyArray(),
        listener: (TqWsConfig) -> Unit,
    ): WebSocket {
        val config = TqWebSocketListenerConfig()
        listener(config)

        return client.newWebSocketBuilder()
            .let {
                var ref = it
                headers.forEach { (k, v) -> ref = ref.header(k, v) }
                if (subprotocols.isNotEmpty()) ref =
                    ref.subprotocols(subprotocols.first(), *subprotocols.drop(1).toTypedArray())
                ref
            }
            .connectTimeout(connectTimeout)
            .buildAsync(URI.create(url), config)
            .join()
    }

    open fun delete(url: String, headers: Map<String, Any> = emptyMap(), timeoutInSec: Long = 30)
        : TqResponse = request("DELETE", url, headers = headers, timeoutInSec = timeoutInSec)

    open fun get(url: String, headers: Map<String, Any> = emptyMap(), timeoutInSec: Long = 30)
        : TqResponse = request("GET", url, headers = headers, timeoutInSec = timeoutInSec)

    open fun patch(url: String, json: Any?, headers: Map<String, Any> = emptyMap(), timeoutInSec: Long = 30)
        : TqResponse = request("PATCH", url, json, headers, timeoutInSec = timeoutInSec)

    open fun put(url: String, json: Any?, headers: Map<String, Any> = emptyMap(), timeoutInSec: Long = 30)
        : TqResponse = request("PUT", url, json, headers, timeoutInSec = timeoutInSec)

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

    open fun <T : Any?> patch(
        url: String,
        json: Any?,
        headers: Map<String, Any> = emptyMap(),
        action: TqResponse.() -> T
    ): T? {
        val response = patch(url, json, headers)
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
        val response: TqResponse
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
                    "PATCH", "PUT", "POST" -> it.method(method, BodyPublishers.ofString(transform(json)))
                    "DELETE" -> it.DELETE()
                    else -> it
                }
            }
            .build()

        response = try {
            val resp = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
            TqResponse(resp.statusCode(), resp.body(), resp.headers().map(), mapper)
        } catch (e: IOException) {
            TqResponse(-1, (e.message ?: "").byteInputStream(), request.headers().map(), mapper)
        } catch (e: InterruptedException) {
            TqResponse(-1, (e.message ?: "").byteInputStream(), request.headers().map(), mapper)
        }
        return response
    }

    private object SelfSignedTrustManager : X509TrustManager {
        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
        override fun getAcceptedIssuers(): Array<out X509Certificate> = emptyArray()
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
    }
}
