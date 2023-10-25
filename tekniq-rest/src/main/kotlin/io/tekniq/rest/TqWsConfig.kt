package io.tekniq.rest

import java.net.http.WebSocket
import java.nio.ByteBuffer

interface TqWsConfig {
    fun onOpen(handler: (WebSocket) -> Unit)
    fun onText(handler: (WebSocket, CharSequence, Boolean) -> Unit)
    fun onBinary(handler: (WebSocket, ByteBuffer, Boolean) -> Unit)
    fun onPing(handler: (WebSocket, ByteBuffer) -> Unit)
    fun onPong(handler: (WebSocket, ByteBuffer) -> Unit)
    fun onClose(handler: (WebSocket, Int, String) -> Unit)
    fun onError(handler: (WebSocket, Throwable) -> Unit)
}
