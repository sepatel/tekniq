package io.tekniq.config

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class TqEnvConfigTest {
    private val subject = TqEnvConfig()

    @Test fun naturalBehavior() {
        Assert.assertTrue(subject.keys.size > 10)

        System.getenv().forEach {
            assertEquals(it.value, subject.get(it.key))
        }
    }
}
