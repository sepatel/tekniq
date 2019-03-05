package io.tekniq.web

import io.tekniq.validation.ValidationException
import spark.*
import kotlin.reflect.KClass

class TqSparklin(config: TqSparklinConfig = TqSparklinConfig(), routes: TqSparklinRoute.() -> Unit) {
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

        val routeHandler = DefaultSparklinRoute(service, config.responseTransformer)
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
        routes(routeHandler)

        service.init()
    }

    fun awaitInitialization() = service.awaitInitialization()
    fun stop() = service.stop()
}

private class DefaultSparklinRoute(val service: Service, val defaultResponseTransformer: ResponseTransformer) : TqSparklinRoute {
    override fun halt(status: Int, message: Any?): HaltException = service.halt(status, defaultResponseTransformer.render(message))

    override fun before(path: String, acceptType: String, filter: (Request, Response) -> Unit) =
            service.before(path, acceptType) { req, res -> filter(req, res) }

    override fun after(path: String, acceptType: String, filter: (Request, Response) -> Unit) =
            service.after(path, acceptType) { req, res -> filter(req, res) }

    override fun afterAfter(path: String, filter: (Request, Response) -> Unit) =
            service.afterAfter(path) { req, res -> filter(req, res) }

    override fun get(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.get(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun post(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.post(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun put(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.put(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun patch(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.patch(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun delete(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.delete(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun head(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.head(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun trace(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.trace(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun connect(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.connect(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun options(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) =
            service.options(path, acceptType, Route(route), transformer ?: defaultResponseTransformer)

    override fun webSocket(path: String, handler: KClass<*>) = service.webSocket(path, handler.java)

    override fun notFound(route: (Request, Response) -> Any?) = service.notFound { req, res ->
        defaultResponseTransformer.render(route(req, res))
    }

    override fun <T : Exception> exception(exceptionClass: KClass<T>, handler: (T, Request, Response) -> Pair<Int, Any>) {
        service.exception(exceptionClass.java) { exception, request, response ->
            val pair = handler(exception as T, request, response)
            response.status(pair.first)
            response.body(defaultResponseTransformer.render(pair.second))
        }
    }
}

