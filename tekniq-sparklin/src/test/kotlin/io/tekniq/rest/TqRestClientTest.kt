package io.tekniq.rest

import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class TqRestClientTest {
    @Test
    fun get() {
        val client = TqRestClient()
        client.get("https://www.google.com") {
            assertEquals(200, status)
        }
    }

    @Ignore
    @Test
    fun failOnInsecureSite() {
        val client = TqRestClient()
        client.get("https://216.58.210.164/finance") {
            assertEquals(-1, status)
        }
    }

    @Ignore
    @Test
    fun getFailsonInsecureSiteDueToPreviousCallDisablingSecurity() {
        failOnInsecureSite()
    }
}
