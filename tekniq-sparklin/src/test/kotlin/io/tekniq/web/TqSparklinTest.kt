package io.tekniq.web

import io.tekniq.rest.TqRestClient
import org.junit.*
import org.junit.Assert.*
import java.util.*


private data class MockSparkRequest(val name: String, val age: Int, val created: Date? = Date())
private data class MockSparkResponse(val color: String, val grade: Int = 42, val found: Date? = Date(), val nullable: String? = null)

class TqSparklinTest {
    val rest = TqRestClient()

    companion object {
        var sparklin: TqSparklin? = null

        @BeforeClass @JvmStatic fun initWebService() {
            sparklin = TqSparklin(SparklinConfig(port = 9998)) {
                before { _, res -> res.header("Content-type", "application/json") }

                get("/blank") { _, _ -> Unit }
                get("/test") { _, _ -> MockSparkResponse("purple", found = Date(4200)) }
                post("/spitback") { req, _ ->
                    val mock = req.jsonAs<MockSparkRequest>()
                    MockSparkResponse(mock.name, mock.age, mock.created)
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
        var response = rest.get("http://localhost:9998/test")
        assertEquals(200, response.status)

        // Should convert json correctly
        assertNotNull(response.body)
        var mock = response.jsonAs<MockSparkResponse>()
        assertEquals("purple", mock.color)
        assertEquals(42, mock.grade)
        assertEquals(4200L, mock.found?.time)

        // Should be a json response header
        assertEquals("application/json", response.header("Content-Type"))

        // Should POST /spitback correclty
        val request = MockSparkRequest("Superman", 69, Date())
        response = rest.post("http://localhost:9998/spitback", request)
        mock = response.jsonAs<MockSparkResponse>()
        assertEquals(request.created, mock.found)
        assertEquals(request.name, mock.color)
        assertNull(mock.nullable)
    }

    @Test fun lambdaTesting() {
        // Shall allow me to return only the grade
        var grade = rest.get("http://localhost:9998/test") {
            jsonAs<MockSparkResponse>().grade
        }
        assertEquals(42, grade)

        // Shall allow me to handle a 404 scenario
        grade = rest.get("http://localhost:9998/xtest") {
            if (status >= 400) {
                -1
            } else {
                jsonAs<MockSparkResponse>().grade
            }
        }
        assertEquals(-1, grade)
    }

    @Test fun arrayResponseBody() {
        val number = rest.post("http://localhost:9998/list", listOf(42, 69, 1942)) {
            jsonAs<Int>()
        }
        assertEquals(42, number)
    }

    @Test fun noResponseBody() {
        val response = rest.get("http://localhost:9998/blank")
        assertEquals("", response.body)
    }
}