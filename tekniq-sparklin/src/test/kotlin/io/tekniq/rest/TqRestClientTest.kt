package io.tekniq.rest

import org.junit.Assert.*
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
        client.get("https://172.217.5.228") {
            assertEquals(-1, status)
        }
    }

    @Ignore
    @Test
    fun ignoreInsecureSite() {
        val client = TqRestClient(allowSelfSigned = true)
        client.get("https://test.tekniq.io/version") {
            assertEquals(200, status)
        }
    }

    @Test
    fun ignoreHostnameValidation() {
        val client = TqRestClient(ignoreHostnameVerifier = true)
        client.get("https://172.217.5.228") {
            assertNotEquals(-1, status)
        }
    }

    @Ignore
    @Test
    fun getFailsOnInsecureSiteDueToPreviousCallDisablingSecurity() {
        failOnInsecureSite()
    }
}
