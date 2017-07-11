package io.tekniq.web

import io.tekniq.rest.TqRestClient
import org.junit.*
import org.junit.Assert.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private data class MockRequest(val name: String, val age: Int, val created: Date? = Date())
private data class MockResponse(val color: String, val grade: Int = 42, val found: Date? = Date(), val nullable: String? = null)

class SparklinTest {
    val rest = TqRestClient()

    companion object {
        var sparklin: Sparklin? = null

        @BeforeClass @JvmStatic fun initWebService() {
            sparklin = Sparklin(SparklinConfig(port = 9999)) {
                before { _, res -> res.header("Content-type", "application/json") }

                get("/blank") { _, _ -> Unit }
                get("/test") { _, _ -> MockResponse("purple", found = Date(4200)) }
                post("/spitback") { req, _ ->
                    val mock = req.jsonAs<MockRequest>()
                    MockResponse(mock.name, mock.age, mock.created)
                }
                post("/validate/simple") { req, _ ->
                    required("name").length("name", min = 4, max = 12).stopOnRejections()
                    req.jsonAs<MockRequest>()
                }
                post("/list") { req, _ ->
                    val list = req.jsonAs<List<Int>>()
                    list.firstOrNull()
                }
            }
        }

        @AfterClass @JvmStatic fun shutdownWebService() {
            sparklin?.stop()
        }
    }

    @Test fun happyWebServiceRequests() {
        // GET /test
        var response = rest.get("http://localhost:9999/test")
        assertEquals(200, response.status)

        // Should convert json correctly
        assertNotNull(response.body)
        var mock = response.jsonAs<MockResponse>()
        assertEquals("purple", mock.color)
        assertEquals(42, mock.grade)
        assertEquals(4200L, mock.found?.time)

        // Should be a json response header
        assertEquals("application/json", response.header("Content-Type"))

        // Should POST /spitback correclty
        val request = MockRequest("Superman", 69, Date())
        response = rest.post("http://localhost:9999/spitback", request)
        mock = response.jsonAs<MockResponse>()
        assertEquals(request.created, mock.found)
        assertEquals(request.name, mock.color)
        assertNull(mock.nullable)
    }

    @Test fun lambdaTesting() {
        // Shall allow me to return only the grade
        var grade = rest.get("http://localhost:9999/test") {
            jsonAs<MockResponse>().grade
        }
        assertEquals(42, grade)

        // Shall allow me to handle a 404 scenario
        grade = rest.get("http://localhost:9999/xtest") {
            if (status >= 400) {
                -1
            } else {
                jsonAs<MockResponse>().grade
            }
        }
        assertEquals(-1, grade)
    }

    @Test fun arrayResponseBody() {
        val number = rest.post("http://localhost:9999/list", listOf(42, 69, 1942)) {
            jsonAs<Int>()
        }
        assertEquals(42, number)
    }

    @Test fun noResponseBody() {
        val response = rest.get("http://localhost:9999/blank")
        assertEquals("", response.body)
    }

    @Test fun basicValidationTesting() {
        rest.post("http://localhost:9999/validate/simple", MockRequest(name = "Freddy", age = 18)) {
            assertEquals(200, status)
        }

        val started = AtomicInteger()
        val responded = AtomicInteger()
        val tested = AtomicInteger()
        val q = mutableListOf<Thread>()
        for (i in 1..50) {
            q += thread {
                started.incrementAndGet()
                rest.post("http://localhost:9999/validate/simple", MockRequest(name = "F", age = 18)) {
                    responded.incrementAndGet()
                    if (status != 400) {
                        println("Invalid response: $this")
                    }
                    assertEquals(400, status)
                    tested.incrementAndGet()
                }
            }
        }

        q.forEach { it.join() }

        assertEquals(50, tested.get())
        assertEquals(started.get(), responded.get())
        assertEquals(responded.get(), tested.get())
    }
}

