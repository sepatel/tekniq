package io.tekniq.rest

import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage

internal class TqWebSocketListenerConfig : TqWsConfig, WebSocket.Listener {
    private lateinit var onOpenHandler: (WebSocket) -> Unit
    private lateinit var onTextHandler: (WebSocket, CharSequence, Boolean) -> Unit
    private lateinit var onBinaryHandler: (WebSocket, ByteBuffer, Boolean) -> Unit
    private lateinit var onPingHandler: (WebSocket, ByteBuffer) -> Unit
    private lateinit var onPongHandler: (WebSocket, ByteBuffer) -> Unit
    private lateinit var onCloseHandler: (WebSocket, Int, String) -> Unit
    private lateinit var onErrorHandler: (WebSocket, Throwable) -> Unit

    override fun onOpen(webSocket: WebSocket) {
        if (::onOpenHandler.isInitialized) onOpenHandler(webSocket)
        super.onOpen(webSocket)
    }

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
        if (::onTextHandler.isInitialized) onTextHandler(webSocket, data, last)
        return super.onText(webSocket, data, last)
    }

    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*> {
        if (::onBinaryHandler.isInitialized) onBinaryHandler(webSocket, data, last)
        return super.onBinary(webSocket, data, last)
    }

    override fun onPing(webSocket: WebSocket, message: ByteBuffer): CompletionStage<*> {
        if (::onPingHandler.isInitialized) onPingHandler(webSocket, message)
        return super.onPing(webSocket, message)
    }

    override fun onPong(webSocket: WebSocket, message: ByteBuffer): CompletionStage<*> {
        if (::onPongHandler.isInitialized) onPongHandler(webSocket, message)
        return super.onPong(webSocket, message)
    }

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
        if (::onCloseHandler.isInitialized) onCloseHandler(webSocket, statusCode, reason)
        return super.onClose(webSocket, statusCode, reason)
    }

    override fun onError(webSocket: WebSocket, error: Throwable) {
        if (::onErrorHandler.isInitialized) onErrorHandler(webSocket, error)
        super.onError(webSocket, error)
    }

    override fun onOpen(handler: (webSocket: WebSocket) -> Unit) {
        onOpenHandler = handler
    }

    override fun onText(handler: (WebSocket, CharSequence, Boolean) -> Unit) {
        onTextHandler = handler
    }

    override fun onBinary(handler: (WebSocket, ByteBuffer, Boolean) -> Unit) {
        onBinaryHandler = handler
    }

    override fun onPing(handler: (WebSocket, ByteBuffer) -> Unit) {
        onPingHandler = handler
    }

    override fun onPong(handler: (WebSocket, ByteBuffer) -> Unit) {
        onPongHandler = handler
    }

    override fun onClose(handler: (WebSocket, Int, String) -> Unit) {
        onCloseHandler = handler
    }

    override fun onError(handler: (WebSocket, Throwable) -> Unit) {
        onErrorHandler = handler
    }
}
