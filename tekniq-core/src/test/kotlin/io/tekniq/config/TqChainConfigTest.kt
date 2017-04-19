package io.tekniq.config

import org.junit.Assert
import org.junit.Test

class TqChainConfigTest {
    @Test fun emptyBackingConfiguration() {
        val config = TqChainConfig()
        Assert.assertEquals(0, config.keys.size)
    }
}
