package io.tekniq.rest

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.concurrent.Flow
import kotlin.reflect.KClass

/**
 * Multiline Event Names and Ids are not supported. Only the first line will be sent for those fields. This is for
 * security reasons where possible injection attacks can take place and the lack of protection at the protocol level for
 * such abuses. Still not a perfect prevent of abuse but might help a little bit.
 */
interface TqSseConfig<T> {
    fun onComment(handler: (String) -> Unit)
    fun onEvent(handler: (T, String?, String?) -> Unit)
    fun onError(handler: (Throwable) -> Unit)
}

internal class TqSseConfigListener<T : Any>(
    private val klazz: KClass<T>,
    private val mapper: ObjectMapper,
) : TqSseConfig<T>, Flow.Subscriber<String> {
    private data class Buffer(
        val id: StringBuilder = StringBuilder(),
        val event: StringBuilder = StringBuilder(),
        val data: StringBuilder = StringBuilder()
    )

    private lateinit var subscription: Flow.Subscription
    private lateinit var onCommentHandler: (String) -> Unit
    private lateinit var onEventHandler: (T, String?, String?) -> Unit
    private lateinit var onErrorHandler: (Throwable) -> Unit
    private var buffer = Buffer()

    override fun onComment(handler: (String) -> Unit) {
        onCommentHandler = handler
    }

    override fun onEvent(handler: (T, String?, String?) -> Unit) {
        onEventHandler = handler
    }

    override fun onError(handler: (Throwable) -> Unit) {
        onErrorHandler = handler
    }

    override fun onSubscribe(subscription: Flow.Subscription) {
        this.subscription = subscription
        this.buffer = Buffer()
        subscription.request(1)
    }

    override fun onError(throwable: Throwable) {
        if (::onErrorHandler.isInitialized) onErrorHandler(throwable)
    }

    override fun onComplete() {
        subscription.cancel()
    }

    override fun onNext(item: String) {
        if (item == "") { // buffer finished send it
            if (::onEventHandler.isInitialized) onEventHandler(
                if (klazz == String::class) buffer.data.toString() as T
                else mapper.readValue(buffer.data.toString(), klazz.java),
                buffer.event.toString(),
                if (buffer.id.isBlank()) null else buffer.id.toString(),
            )
            this.buffer = Buffer()
        }
        when {
            item.startsWith(": ") -> if (::onCommentHandler.isInitialized) onCommentHandler(item.substring(": ".length))
            item.startsWith("id: ") -> buffer.id.append(item.substring("id: ".length))
            item.startsWith("event: ") -> buffer.event.append(item.substring("event: ".length))
            item.startsWith("data: ") -> {
                if (buffer.data.isNotEmpty()) buffer.data.appendLine()
                buffer.data.append(item.substring("data: ".length))
            }
        }
        subscription.request(1)
    }
}
