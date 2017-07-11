package io.tekniq.web

import io.tekniq.validation.ValidationException
import spark.*
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass

class TqSparklin(config: SparklinConfig = SparklinConfig(), routes: TqSparklinRoute.() -> Unit) {
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
        routes.invoke(routeHandler)

        service.init()
    }

    fun awaitInitialization() = service.awaitInitialization()
    fun halt(status: Int = HttpServletResponse.SC_OK, message: String? = null) = service.halt(status, message)
    fun stop() = service.stop()
}

private class DefaultSparklinRoute(val service: Service, val defaultResponseTransformer: ResponseTransformer) : TqSparklinRoute {
    override fun before(path: String, acceptType: String, filter: (Request, Response) -> Unit) {
        val innerFilter = Filter { req, res -> filter.invoke(req, res) }
        service.before(path, acceptType, innerFilter)
    }

    override fun after(path: String, acceptType: String, filter: (Request, Response) -> Unit) {
        val innerFilter = Filter { req, res -> filter.invoke(req, res) }
        service.after(path, acceptType, innerFilter)
    }

    override fun get(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.get(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun post(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.post(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun put(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.put(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun patch(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.patch(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun delete(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.delete(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun head(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.head(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun trace(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.trace(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun connect(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
        service.connect(path, acceptType, innerRoute, transformer ?: defaultResponseTransformer)
    }

    override fun options(path: String, acceptType: String, transformer: ResponseTransformer?, route: (Request, Response) -> Any?) {
        val innerRoute = Route { req, res -> route.invoke(req, res) }
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

