package io.tekniq.rest

import io.javalin.Javalin
import io.javalin.http.HttpStatus
import io.javalin.http.bodyAsClass
import io.javalin.http.sse.SseClient
import io.javalin.json.JavalinJackson
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

object TqRestClientSpec : DescribeSpec({
    val rest = TqRestClient()
    val baseUrl = "http://localhost:23456"
    beforeSpec {
        println("***** Starting Javalin")
        TqRestClientSpec.javalin.start(23456)
    }
    afterSpec {
        println("***** Stopping Javalin")
        TqRestClientSpec.javalin.stop()
    }
    describe("Excessive Thread Count") {
        val url = "$baseUrl/get"
        it("Shall not increase in thread counts") {
            rest.get(url) { body } // warmup
            val active = Thread.activeCount()

            repeat(5) {
                rest.get(url) { body }
                Thread.activeCount() shouldBeExactly active
            }
        }
    }
    describe("Basic functionality") {
        it("GET on postman echo") {
            val url = "$baseUrl/get"
            val resp = rest.get(url)
            assertEquals(200, resp.status)
            val echo = resp.jsonAs<PostmanEcho>()
            assertEquals(url, echo.url)
        }

        it("POST on postman echo") {
            val url = "$baseUrl/post"
            val resp = rest.post(url, mapOf("a" to "apple", "c" to "candy"))
            assertEquals(200, resp.status)
            val echo = resp.jsonAs<PostmanEcho>()
            assertEquals(url, echo.url)
            assertEquals("apple", echo.json["a"])
        }

        it("Should timeout a request") {
            val url = "$baseUrl/timeout"
            val resp = rest.get(url, timeoutInSec = 1)
            assertEquals(-1, resp.status)
        }
    }

    describe("Streaming functionality") {
        it("Should stream data back as well") {
            val url = "$baseUrl/stream"
            val resp = rest.post(url, "true")
            assertEquals(200, resp.status)
            val results = resp.jsonArrayOf<SimpleData>().asSequence().toList()
            assertEquals(4, results.size)
            assertEquals("Two", results[1].name)
        }
    }

    describe("Server Side Events") {
        it("Should handle simple string events") {
            val future = rest.sseListener<String>("$baseUrl/sse/string") {
                it.onEvent { data, event, id -> println("[Client Event] $data ($event) [$id]") }
                it.onError { println("{Client Error} ${it.message}") }
            }
            future.join() shouldBe 200
        }
        it("Should handle object events") {
            val future = rest.sseListener<SimpleData>("$baseUrl/sse/object") {
                it.onEvent { data, event, id -> println("[Client Event] $data ($event) [$id]") }
                it.onError { println("{Client Error} ${it.message}") }
            }
            future.join() shouldBe 200
        }
    }

    describe("Websocket Interaction") {
        it("should connect, send and receive messages, and hangup") {
            async {
                val ws = rest.openWebSocket("${baseUrl.replace("http://", "ws://")}/ws", mapOf("x-test" to "purple")) {
                    it.onText { _, charSequence, b -> println("[Client Message $b] $charSequence") }
                    it.onError { _, t -> t.printStackTrace() }
                }
                ws.sendText("It's the\nend of the world", false)
                ws.sendText("and I feel\nfine", true)
                ws.sendClose(1003, "Funny thing")
                delay(1.seconds)
            }.await()
        }
    }
}) {
    private val clients = ConcurrentHashMap.newKeySet<MessageClient<*>>()
    private val rest = TqRestClient()
    private val javalin = Javalin
        .create {
            it.jsonMapper(JavalinJackson(rest.mapper))
            it.bundledPlugins.enableCors { cors -> cors.addRule { it.anyHost() } }
        }
        .get("/get") { it.json(PostmanEcho(it.queryParamMap(), it.headerMap(), it.url())) }
        .post("/post") { it.json(PostmanEcho(it.queryParamMap(), it.headerMap(), it.url(), it.bodyAsClass())) }
        .get("/timeout") {
                Thread.sleep(2.seconds.inWholeMilliseconds)
                it.status(HttpStatus.NO_CONTENT)
            }
        .post("/stream") {
                val list = listOf("One", "Two", "Three", "Four")
                val iterator = object : Iterator<SimpleData> {
                    private var i = list.iterator()
                    override fun hasNext(): Boolean = i.hasNext()

                    override fun next(): SimpleData {
                        if (!i.hasNext()) throw NoSuchElementException()
                        return SimpleData(i.next(), mapOf("Random" to Random.nextDouble()))
                    }
                }
                it.jsonStream(iterator)
            }
        .sse("/sse/string") { client ->
            clients += MessageClient(
                client, """
                This is a super long message with a substructure
                id: fake-random-id
                event: fake-event
                data: fake simple message
                
                Now what to do about it all?
            """.trimIndent()
            )
            client.onClose { clients.removeIf { it.sse == client } }
            thread {
                Thread.sleep(1000)
                client.close()
            }
            client.keepAlive()
        }
        .sse("/sse/object") { client ->
            clients += MessageClient(client, SimpleData("Blob", mapOf("iq" to 142)))
            client.onClose { clients.removeIf { it.sse == client } }
            thread {
                Thread.sleep(1000)
                client.close()
            }
            client.keepAlive()
        }
        .ws("/ws") { config ->
            config.onConnect {
                it.send(SimpleData("Guardians", mapOf("Location" to "Galaxy")).toString())
            }
            config.onMessage {
                println("[Server Message] ${it.message()}")
                it.send(SimpleData(it.message(), it.queryParamMap()).toString())
                it.closeSession(1002, "I'm a happy camper")
            }
            config.onError { it.error()?.printStackTrace() }
            config.onClose { println("Closed because of ${it.status()} - ${it.reason()}") }
        }

    init {
        fixedRateTimer(period = 250) {
            clients.forEach { it.send() }
        }
    }

    private data class PostmanEcho(
        val args: Map<String, Any> = emptyMap(),
        val headers: Map<String, Any> = emptyMap(),
        val url: String,
        val json: Map<String, Any> = emptyMap(),
    )

    private data class SimpleData(val name: String, val meta: Map<String, Any>)
    private data class MessageClient<T : Any>(val sse: SseClient, val msg: T) {
        fun send() {
            println("[Server Side] Sending message for $sse=$msg")
            sse.sendEvent(msg)
            sse.sendComment("This is a funny COMMENT as well\nThat spans multiple lines maybe?")
            sse.sendEvent("Fiddle", msg)
            sse.sendEvent("Fiddle", msg, "random-id")
        }
    }
}
