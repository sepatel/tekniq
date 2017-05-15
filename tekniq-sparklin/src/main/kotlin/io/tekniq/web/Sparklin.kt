package io.tekniq.web

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import io.tekniq.validation.*
import spark.*
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass

class Sparklin(config: SparklinConfig = SparklinConfig(), routes: SparklinRoute.() -> Unit) {
    private val service: Service = Service.ignite()

    init {
        service.ipAddress(config.ip)
        service.port(config.port)
        service.threadPool(config.maxThreads, config.minThreads, config.idleTimeoutMillis)
        config.webSocketTimeout?.let { service.webSocketIdleTimeoutMillis(it) }
        config.keystore?.let { service.secure(it.keystoreFile, it.keystorePassword, it.truststoreFile, it.truststorePassword) }
        config.staticFiles?.fileLocation?.let { service.staticFileLocation(it) }
        config.staticFiles?.externalFileLocation?.let { service.externalStaticFileLocation(it) }
        config.staticFiles?.headers?.let { service.staticFiles }

        val routeHandler = DefaultRoute(service, config.authorizationManager, config.responseTransformer)
        routeHandler.before { request, _ -> request.attribute(BODY_CACHE, request.body()) }
        routeHandler.exception(ValidationException::class) { e, _, _ ->
            Pair(400, mapOf("errors" to e.rejections, "data" to e.data).filter { it.value != null })
        }
        routeHandler.exception(NotAuthorizedException::class) { e, _, _ ->
            Pair(401, mapOf("errors" to e.rejections, "type" to if (e.all) {
                "ALL"
            } else {
                "OR"
            }))
        }
        routes.invoke(routeHandler)

        service.init()
    }

    fun awaitInitialization() = service.awaitInitialization()
    fun halt(status: Int = HttpServletResponse.SC_OK, message: String? = null) = service.halt(status, message)
    fun stop() = service.stop()
}

private class DefaultRoute(val service: Service, val authorizationManager: AuthorizationManager?, val defaultResponseTransformer: ResponseTransformer) : SparklinRoute {
    override fun before(path: String, acceptType: String, filter: SparklinValidation.(Request, Response) -> Unit) {
        val innerFilter = Filter { req, res -> filter.invoke(WebValidation(req, authorizationManager), req, res) }
        service.before(path, acceptType, innerFilter)
    }

    override fun after(path: String, acceptType: String, filter: SparklinValidation.(Request, Response) -> Unit) {
        val innerFilter = Filter { req, res -> filter.invoke(WebValidation(req, authorizationManager), req, res) }
        service.after(path, acceptType, innerFilter)
    }

    override fun get(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.get(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun post(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.post(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun put(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.put(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun patch(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.patch(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun delete(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.delete(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun head(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.head(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun trace(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.trace(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun connect(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.connect(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun options(path: String, acceptType: String, transformer: ResponseTransformer?, route: SparklinValidation.(Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(WebValidation(req, authorizationManager), req, res) }
        service.options(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun webSocket(path: String, handler: KClass<*>) = service.webSocket(path, handler.java)

    override fun <T : Exception> exception(exceptionClass: KClass<T>, handler: (T, Request, Response) -> Pair<Int, Any>) {
        @Suppress("UNCHECKED_CAST")
        val innerHandler: ExceptionHandler = ExceptionHandler { exception, request, response ->
            val pair = handler.invoke(exception as T, request, response)
            response.status(pair.first)
            response.body(defaultResponseTransformer.render(pair.second))
        }
        service.exception(exceptionClass.java, innerHandler)
    }
}

private class WebValidation(private val req: Request, private val authorizationManager: AuthorizationManager?) : SparklinValidation({
    when (req.attribute<String?>(BODY_CACHE).isNullOrBlank()) { // only attempt if there is even anything worth attempting
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
