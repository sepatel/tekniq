package io.tekniq.rest

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.ints.shouldBeLessThan
import kotlin.test.assertEquals

object TqRestClientSpec : DescribeSpec({
    describe("Excessive Thread Count") {
        val url = "https://postman-echo.com/get"

        it("Shall not increase in thread counts") {
            val rest = TqRestClient()
            rest.get( url) { body } // warmup
            val active = Thread.activeCount()

            repeat(5) {
                rest.get( url) { body }
                Thread.activeCount() shouldBeExactly active
            }
        }
    }
    describe("Basic functionality") {
        it("GET on postman echo") {
            val url = "https://postman-echo.com/get"
            val rest = TqRestClient()
            val resp = rest.get(url)
            assertEquals(200, resp.status)
            val echo = resp.jsonAs<PostmanEcho>()
            assertEquals(url, echo.url)
        }

        it("POST on postman echo") {
            val url = "https://postman-echo.com/post"
            val rest = TqRestClient()
            val resp = rest.post(url, mapOf("a" to "apple", "c" to "candy"))
//            println(resp)
            assertEquals(200, resp.status)
            val echo = resp.jsonAs<PostmanEcho>()
//            println(echo)
            assertEquals(url, echo.url)
            assertEquals("apple", echo.json["a"])
        }

        it("Should timeout a request") {
            val url = "https://httpstat.us/200?sleep=2000" // 2 second delay with status 200 on success
            val rest = TqRestClient()
            val resp = rest.get(url, timeoutInSec = 1)
            assertEquals(-1, resp.status)
        }
    }
//
//    describe("Streaming functionality") {
//        it("Should stream data back as well") {
//            data class SimpleData(val name: String, val meta: Map<String, Any>)
//            val url = "https://localhost:2223/api/test"
//            val rest = TqRestClient(allowSelfSigned = true)
//            val resp = rest.post(url, "true")
//            assertEquals(200, resp.status)
//            resp.jsonArrayOf<SimpleData>().forEach {
//                println(it)
//            }
//        }
//    }
}) {
    data class PostmanEcho(
        val args: Map<String, Any> = emptyMap(),
        val headers: Map<String, Any> = emptyMap(),
        val url: String,
        val json: Map<String, Any> = emptyMap(),
    )
}
