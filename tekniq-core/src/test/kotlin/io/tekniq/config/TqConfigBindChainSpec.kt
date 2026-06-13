package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

data class ChainedApp(
    val name: String,
    val port: Int,
    val enabled: Boolean = true
)

object TqConfigBindChainSpec : DescribeSpec({
    describe("binding from TqChainConfig") {
        it("uses the first config that contains the key") {
            val a = TqMapConfig(mapOf("name" to "from-a"))
            val b = TqMapConfig(mapOf("name" to "from-b", "port" to 9000))
            val chain = TqChainConfig(a, b)
            val bound = chain.bind<ChainedApp>()
            bound shouldBe ChainedApp(name = "from-a", port = 9000)
        }

        it("falls through to next config when a key is missing") {
            val a = TqMapConfig(mapOf<String, Any>("name" to "from-a"))
            val b = TqMapConfig(mapOf("port" to 9000))
            val chain = TqChainConfig(a, b)
            val bound = chain.bind<ChainedApp>()
            bound shouldBe ChainedApp(name = "from-a", port = 9000)
        }

        it("throws when a required key is missing across all layers") {
            val a = TqMapConfig(mapOf<String, Any>("name" to "x"))
            val b = TqMapConfig(mapOf<String, Any>("enabled" to false))
            val chain = TqChainConfig(a, b)
            val ex = runCatching { chain.bind<ChainedApp>() }.exceptionOrNull()
            (ex is TqConfigBindException) shouldBe true
            (ex as TqConfigBindException).path shouldBe "port"
        }

        it("uses @TqConfigPrefix when binding through a chain") {
            val a = TqMapConfig(mapOf("server.host" to "x", "server.port" to 9000))
            val b = TqMapConfig(mapOf("server.host" to "y"))
            val chain = TqChainConfig(a, b)
            val bound = chain.bind<AnnotatedConfig>()
            bound shouldBe AnnotatedConfig(host = "x", port = 9000)
        }
    }
})
