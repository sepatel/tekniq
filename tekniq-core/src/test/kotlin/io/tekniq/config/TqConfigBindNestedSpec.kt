package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

data class PoolConfig(
    val min: Int = 0,
    val max: Int = 100,
    val acquireTimeoutMs: Long = 30_000
)

data class ServerConfig(
    val host: String,
    val port: Int = 8080,
    val pool: PoolConfig = PoolConfig()
)

data class WithList(
    val name: String,
    val tags: List<String> = emptyList()
)

data class WithIntList(
    val numbers: List<Int> = emptyList()
)

data class WithMap(
    val options: Map<String, String> = emptyMap()
)

data class WithIntMap(
    val limits: Map<String, Int> = emptyMap()
)

data class WithNestedList(
    val host: String,
    val pools: List<PoolConfig> = emptyList()
)

data class DeepNested(
    val level1: String,
    val child: DeepNestedChild
)

data class DeepNestedChild(
    val level2: String,
    val grandchild: DeepNestedGrandchild
)

data class DeepNestedGrandchild(
    val level3: String
)

object TqConfigBindNestedSpec : DescribeSpec({
    describe("nested data class binding") {
        it("binds nested data class with dot-notation keys") {
            val config = TqMapConfig(mapOf(
                "host" to "localhost",
                "pool.min" to 2,
                "pool.max" to 20
            ))
            val bound = config.bind<ServerConfig>()
            bound shouldBe ServerConfig(host = "localhost", port = 8080, pool = PoolConfig(2, 20))
        }

        it("binds nested data class from nested Map structure") {
            val config = TqMapConfig(mapOf(
                "host" to "x",
                "pool" to mapOf<String, Any>("min" to 5, "max" to 50)
            ))
            val bound = config.bind<ServerConfig>()
            bound shouldBe ServerConfig(host = "x", pool = PoolConfig(5, 50))
        }

        it("binds deeply nested data class (3 levels)") {
            val config = TqMapConfig(mapOf(
                "level1" to "L1",
                "child.level2" to "L2",
                "child.grandchild.level3" to "L3"
            ))
            val bound = config.bind<DeepNested>()
            bound shouldBe DeepNested(
                level1 = "L1",
                child = DeepNestedChild(
                    level2 = "L2",
                    grandchild = DeepNestedGrandchild(level3 = "L3")
                )
            )
        }
    }

    describe("List binding") {
        it("binds List<String> from a JSON-style list") {
            val config = TqMapConfig(mapOf(
                "name" to "alice",
                "tags" to listOf("primary", "replica")
            ))
            val bound = config.bind<WithList>()
            bound shouldBe WithList("alice", listOf("primary", "replica"))
        }

        it("binds List<String> from a comma-separated string (properties style)") {
            val config = TqMapConfig(mapOf(
                "name" to "alice",
                "tags" to "primary, replica ,failover"
            ))
            val bound = config.bind<WithList>()
            bound shouldBe WithList("alice", listOf("primary", "replica", "failover"))
        }

        it("binds List<Int> with element coercion") {
            val config = TqMapConfig(mapOf(
                "numbers" to listOf("1", "2", "3")
            ))
            val bound = config.bind<WithIntList>()
            bound shouldBe WithIntList(listOf(1, 2, 3))
        }

        it("binds empty list when key is missing and field has default") {
            val config = TqMapConfig(mapOf("name" to "alice"))
            val bound = config.bind<WithList>()
            bound shouldBe WithList(name = "alice", tags = emptyList())
        }

        it("binds List<DataClass> from JSON list of maps") {
            val config = TqMapConfig(mapOf(
                "host" to "x",
                "pools" to listOf(
                    mapOf<String, Any>("min" to 1, "max" to 10),
                    mapOf<String, Any>("min" to 5, "max" to 50)
                )
            ))
            val bound = config.bind<WithNestedList>()
            bound shouldBe WithNestedList(
                host = "x",
                pools = listOf(
                    PoolConfig(min = 1, max = 10),
                    PoolConfig(min = 5, max = 50)
                )
            )
        }
    }

    describe("Map<String, *> binding") {
        it("binds Map<String, String> from nested JSON-style map") {
            val config = TqMapConfig(mapOf(
                "options" to mapOf<String, Any>("timeout" to "5000", "appName" to "svc")
            ))
            val bound = config.bind<WithMap>()
            bound shouldBe WithMap(mapOf("timeout" to "5000", "appName" to "svc"))
        }

        it("binds Map<String, String> from prefix-scanned flat keys (properties style)") {
            val config = TqMapConfig(mapOf(
                "options.timeout" to "5000",
                "options.appName" to "svc"
            ))
            val bound = config.bind<WithMap>()
            bound shouldBe WithMap(mapOf("timeout" to "5000", "appName" to "svc"))
        }

        it("binds empty map when no keys match the prefix") {
            val config = TqMapConfig(emptyMap<String, Any>())
            val bound = config.bind<WithMap>()
            bound shouldBe WithMap()
        }

        it("binds Map<String, Int> with value coercion") {
            val config = TqMapConfig(mapOf(
                "limits.max" to "100",
                "limits.min" to "1"
            ))
            val bound = config.bind<WithIntMap>()
            bound shouldBe WithIntMap(mapOf("max" to 100, "min" to 1))
        }
    }
})
