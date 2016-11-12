package io.tekniq.config

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.junit.Assert

class TqChainConfigTest : Spek({
    describe("An empty backing configuration") {
        val config = TqChainConfig()
        Assert.assertEquals(0, config.keys.size)
    }
})
