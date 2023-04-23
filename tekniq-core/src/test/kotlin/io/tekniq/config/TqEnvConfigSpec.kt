package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object TqEnvConfigSpec : DescribeSpec({
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
