package io.tekniq.web

import io.tekniq.rest.TqRestClient
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.*
import java.util.*

private data class MockRequest(val name: String, val age: Int, val created: Date? = Date())
private data class MockResponse(val color: String, val grade: Int = 42, val found: Date? = Date(), val nullable: String? = null)

class SparklinTest : Spek({
    val rest = TqRestClient()
    var sparklin: Sparklin? = null

    describe("Initialize the Web Service") {
        sparklin = Sparklin(SparklinConfig(port = 9999)) {
            before { req, res -> res.header("Content-type", "application/json") }

            get("/test") { req, res -> MockResponse("purple", found = Date(4200)) }
            post("/spitback") { req, res ->
                val mock = req.jsonAs<MockRequest>() ?: return@post null
                MockResponse(mock.name, mock.age, mock.created)
            }
        }
    }

    describe("Happy Web Service Requests") {
        describe("GET /test") {
            val response = rest.get("http://localhost:9999/test")
            it("Should be a successful 200 status code") {
                assertEquals(200, response.status)
            }
            it("Should content correctly") {
                assertNotNull(response.json)
                val mock = response.jsonAs<MockResponse>()
                assertEquals("purple", mock.color)
                assertEquals(42, mock.grade)
                assertEquals(4200L, mock.found?.time)
            }
            it("Should be a json response header") {
                assertEquals("application/json", response.header("Content-Type"))
            }
        }

        it("Should POST /spitback correclty") {
            val request = MockRequest("Superman", 69, Date())
            val response = rest.post("http://localhost:9999/spitback", request)
            val mock = response.jsonAs<MockResponse>()
            assertEquals(request.created, mock.found)
            assertEquals(request.name, mock.color)
            assertNull(mock.nullable)
        }
    }

    describe("Shutting down the Web Service") {
        it("Should shutdown without issues") {
            assertNotNull(sparklin)
            sparklin?.stop()
        }
    }
})

