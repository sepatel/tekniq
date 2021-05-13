package io.tekniq.config

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object TqEnvConfigSpek : Spek({
    val subject = TqEnvConfig()

    describe("natural behavior") {
        it("captures all environment variables exactly") {
            assertTrue(subject.keys.size > 10)

            System.getenv().forEach {
                assertEquals(it.value, subject.get(it.key))
            }
        }
    }
})
