package io.tekniq.web

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.tekniq.validation.*
import spark.*
import spark.utils.SparkUtils
import java.util.*
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass

val sparklinMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private val BODY_CACHE = "__${UUID.randomUUID()}"

open class NotAuthorizedException(rejections: Collection<Rejection>, val all: Boolean = true) : ValidationException(rejections)

interface AuthorizationManager {
    /**
     * Must return an empty list if no access is to be granted. Best practice says to return 'AUTHENTICATED' if the user
     * is authenticated and to return 'ANONYMOUS' if the user is not authenticated in addition to normal authorizations
     * that the user may possess.
     */
    fun getAuthz(request: Request): Collection<String>
}

@Deprecated("Will be removed when Sparklin is removed")
interface SparklinRoute {
    fun halt(status: Int = HttpServletResponse.SC_OK, message: Any? = null): HaltException
    fun before(path: String = SparkUtils.ALL_PATHS, acceptType: String = "*/*", filter: SparklinValidation.(Request, Response) -> Unit)
    fun after(path: String = SparkUtils.ALL_PATHS, acceptType: String = "*/*", filter: (Request, Response) -> Unit)
    fun afterAfter(path: String = SparkUtils.ALL_PATHS, filter: (Request, Response) -> Unit)
    fun get(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun post(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun put(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun patch(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun delete(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun head(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun trace(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun connect(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun options(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: SparklinValidation.(Request, Response) -> Any?)
    fun webSocket(path: String, handler: KClass<*>)
    fun notFound(route: (Request, Response) -> Any?)
    //fun internalServerError(route: (Request, Response) -> Unit)

    fun <T : Exception> exception(exceptionClass: KClass<T>, handler: (T, Request, Response) -> Pair<Int, Any>)
}

interface TqSparklinRoute {
    fun halt(status: Int = HttpServletResponse.SC_OK, message: Any? = null): HaltException
    fun before(path: String = SparkUtils.ALL_PATHS, acceptType: String = "*/*", filter: (Request, Response) -> Unit)
    fun after(path: String = SparkUtils.ALL_PATHS, acceptType: String = "*/*", filter: (Request, Response) -> Unit)
    fun afterAfter(path: String = SparkUtils.ALL_PATHS, filter: (Request, Response) -> Unit)
    fun get(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun post(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun put(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun patch(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun delete(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun head(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun trace(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun connect(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun options(path: String, acceptType: String = "*/*", transformer: ResponseTransformer? = null, route: (Request, Response) -> Any?)
    fun webSocket(path: String, handler: KClass<*>)
    fun notFound(route: (Request, Response) -> Any?)
    //fun internalServerError(route: (Request, Response) -> Unit)

    fun <T : Exception> exception(exceptionClass: KClass<T>, handler: (T, Request, Response) -> Pair<Int, Any>)
}

fun Request.bodyCached(): String? {
    if (!this.attributes().contains(BODY_CACHE)) {
        this.attribute(BODY_CACHE, this.body())
    }
    return attribute(BODY_CACHE)
}

inline fun <reified T : Any> Request.jsonAs(): T = jsonAsNullable(T::class)!!
inline fun <reified T : Any> Request.jsonAsNullable(): T? = jsonAsNullable(T::class)
fun <T : Any> Request.jsonAsNullable(type: KClass<T>): T? {
    val body = this.bodyCached()
    if (body.isNullOrBlank()) {
        throw IllegalStateException("No data available to transform")
    }
    return sparklinMapper.readValue(body, type.java)
}

@Deprecated("Please use TqSparklinConfig instead")
typealias SparklinConfig = TqSparklinConfig
data class TqSparklinConfig(
        val ip: String = "0.0.0.0", val port: Int = 4567,
        @Deprecated("Only valid with Sparklin implementation which will be phased out")
        val authorizationManager: AuthorizationManager? = null,
        val responseTransformer: ResponseTransformer = JsonResponseTransformer,
        val idleTimeoutMillis: Int = -1, val webSocketTimeout: Int? = null,
        val maxThreads: Int = -1, val minThreads: Int = -1,
        val keystore: SparklinKeystore? = null,
        val staticFiles: SparklinStaticFiles? = null)

@Deprecated("Please use TqSparklinKeystore instead")
typealias SparklinKeystore = TqSparklinKeystore
data class TqSparklinKeystore(val keystoreFile: String, val keystorePassword: String,
                              val truststoreFile: String, val truststorePassword: String)

@Deprecated("Please use TqSparklinStaticFiles instead")
typealias SparklinStaticFiles = TqSparklinStaticFiles
data class TqSparklinStaticFiles(val fileLocation: String? = null, val externalFileLocation: String? = null,
                                 val headers: Map<String, String> = emptyMap(), val expireInSeconds: Int = 1)

@Deprecated("Please use TqSparklinValidation instead")
typealias SparklinValidation = TqSparklinValidation
abstract class TqSparklinValidation(src: Any?, path: String = "") : TqValidation(src, path) {
    abstract fun authz(vararg authz: String, all: Boolean = true): TqSparklinValidation
}

data class JsonRequestValidation(private val req: Request, private val authorizationManager: AuthorizationManager? = null) : TqSparklinValidation({
    when (req.bodyCached().isNullOrBlank()) { // only attempt if there is even anything worth attempting
        true -> null
        false -> try {
            req.jsonAs<Map<*, *>>()
        } catch (e: JsonMappingException) {
            null
        } catch (e: JsonParseException) {
            null
        }
    }
}()) {
    override fun authz(vararg authz: String, all: Boolean): SparklinValidation {
        val userAuthzList = authorizationManager?.getAuthz(req) ?: emptyList()
        authz.forEach {
            val contains = userAuthzList.contains(it)
            if (all && !contains) {
                throw NotAuthorizedException(mutableListOf(Rejection("unauthorized", it)), all)
            } else if (!all && contains) {
                return this
            }
        }

        if (!all) {
            val rejections = mutableListOf<Rejection>()
            authz.forEach { rejections.add(Rejection("unauthorized", it)) }
            throw NotAuthorizedException(rejections, all)
        }
        return this
    }
}

private object JsonResponseTransformer : ResponseTransformer {
    override fun render(model: Any?): String = when (model) {
        is Unit -> ""
        else -> sparklinMapper.writeValueAsString(model)
    }
}
