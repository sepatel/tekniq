package io.tekniq.config

import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert
import org.junit.Assert.assertEquals

class TqEnvConfigTest : SubjectSpek<TqEnvConfig>({
    subject { TqEnvConfig() }

    describe("natural behavior of TqEnvConfig") {
        it("should have over 10 environment variables before any reading of config starts") {
            Assert.assertTrue(subject.keys.size > 10)
        }

        it("should match all key/values correctly") {
            System.getenv().forEach {
                assertEquals(it.value, subject.get(it.key))
            }
        }
    }
})
