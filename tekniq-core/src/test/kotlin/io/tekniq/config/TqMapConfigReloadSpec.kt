package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object TqMapConfigReloadSpec : DescribeSpec({
    describe("TqMapConfig reload semantics") {
        it("should update values on reload") {
            val initialMap = mapOf("key1" to "value1", "key2" to "value2")
            val config = TqMapConfig(initialMap)

            assertEquals("value1", config.get<String>("key1"))
            assertEquals("value2", config.get<String>("key2"))

            val newMap = mapOf("key1" to "newValue1", "key3" to "value3")
            config.reloadWithMap(newMap)

            assertEquals("newValue1", config.get<String>("key1"))
            assertEquals("value3", config.get<String>("key3"))
            assertNull(config.get<String>("key2"))
        }

        it("should remove keys that are no longer in the map") {
            val initialMap = mapOf("key1" to "value1", "key2" to "value2")
            val config = TqMapConfig(initialMap)

            assertTrue(config.contains("key1"))
            assertTrue(config.contains("key2"))

            val newMap = mapOf("key1" to "newValue1")
            config.reloadWithMap(newMap)

            assertTrue(config.contains("key1"))
            assertFalse(config.contains("key2"))
        }

        it("should correctly update changed values") {
            val initialMap = mapOf("counter" to 1)
            val config = TqMapConfig(initialMap)

            assertEquals(1, config.getInt("counter"))

            config.reloadWithMap(mapOf("counter" to 42))

            assertEquals(42, config.getInt("counter"))
        }

        it("should handle empty reload") {
            val initialMap = mapOf("key1" to "value1")
            val config = TqMapConfig(initialMap)

            config.reloadWithMap(emptyMap())

            assertFalse(config.contains("key1"))
            assertEquals(0, config.keys.size)
        }

        it("should reload using no-arg reload method") {
            val initialMap = mapOf("key1" to "value1")
            val config = TqMapConfig(initialMap)

            assertEquals("value1", config.get<String>("key1"))

            config.reload()

            assertEquals("value1", config.get<String>("key1"))
        }
    }
})