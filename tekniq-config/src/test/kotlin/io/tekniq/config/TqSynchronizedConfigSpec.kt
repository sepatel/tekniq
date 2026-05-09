package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object TqSynchronizedConfigSpec : DescribeSpec({
    describe("TqSynchronizedConfig wrapper") {
        it("should delegate getValue calls") {
            val baseConfig = TqMapConfig(mapOf("key" to "value"))
            val syncConfig = TqSynchronizedConfig(baseConfig)

            syncConfig.get<String>("key") shouldBe "value"
        }

        it("should delegate contains calls") {
            val baseConfig = TqMapConfig(mapOf("key" to "value"))
            val syncConfig = TqSynchronizedConfig(baseConfig)

            assertTrue(syncConfig.contains("key"))
            assertFalse(syncConfig.contains("nonexistent"))
        }

        it("should delegate reload calls") {
            val baseConfig = TqMapConfig(mapOf("key" to "value"))
            val syncConfig = TqSynchronizedConfig(baseConfig)

            syncConfig.reload(mapOf("key" to "newValue", "key2" to "value2"))

            assertEquals("newValue", syncConfig.get<String>("key"))
            assertEquals("value2", syncConfig.get<String>("key2"))
        }
    }

    describe("TqSynchronizedConfig thread safety") {
        it("should allow concurrent reads") {
            val baseConfig = TqMapConfig(mapOf("key" to "initial"))
            val syncConfig = TqSynchronizedConfig(baseConfig)

            val readCount = 100
            val latch = CountDownLatch(readCount)
            val readsCompleted = AtomicInteger(0)

            repeat(readCount) {
                Thread {
                    syncConfig.get<String>("key")
                    readsCompleted.incrementAndGet()
                    latch.countDown()
                }.start()
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertEquals(readCount, readsCompleted.get())
        }

        it("should handle multiple concurrent readers and writers") {
            val baseConfig = TqMapConfig((1..100).associate { "key$it" to "value$it" })
            val syncConfig = TqSynchronizedConfig(baseConfig)

            val operations = 50
            val latch = CountDownLatch(operations * 2)
            val errors = AtomicInteger(0)

            repeat(operations) {
                Thread {
                    try {
                        syncConfig.get<String>("key$it")
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }.start()

                Thread {
                    try {
                        syncConfig.reload(mapOf("key$it" to "updated$it"))
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }.start()
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS))
            assertEquals(0, errors.get())
        }
    }

    describe("TqSynchronizedConfig extends TqConfig") {
        it("should be instance of TqConfig") {
            val baseConfig = TqMapConfig(mapOf("key" to "value"))
            val syncConfig = TqSynchronizedConfig(baseConfig)
            syncConfig.shouldBeInstanceOf<TqConfig>()
        }
    }
})