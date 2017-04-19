package io.tekniq.config

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class TqEnvConfigTest {
    val subject = TqEnvConfig()

    @Test fun naturlaBehavior() {
        Assert.assertTrue(subject.keys.size > 10)

        System.getenv().forEach {
            assertEquals(it.value, subject.get(it.key))
        }
    }
}
