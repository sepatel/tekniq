package io.tekniq.config

import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.assertEquals

class TqEnvConfigTest : SubjectSpek<TqEnvConfig>({
    subject { TqEnvConfig() }

    describe("natural behavior of TqEnvConfig") {
        it("should match all key/values correctly") {
            System.getenv().forEach {
                assertEquals(it.value, subject.get(it.key))
            }
        }
    }
})
