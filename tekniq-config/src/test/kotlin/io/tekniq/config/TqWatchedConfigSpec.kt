package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object TqWatchedConfigSpec : DescribeSpec({
    val tempDir = File(System.getProperty("java.io.tmpdir"), "tekniq-watch-test-${UUID.randomUUID()}").also { it.mkdirs() }

    afterSpec { tempDir.deleteRecursively() }

    describe("TqWatchedConfig file watching") {
        it("should be instance of TqWatchedConfig") {
            val config = TqMapConfig(mapOf("key" to "value"))
            val watched = TqWatchedConfig(config, tempDir.absolutePath + "/config.properties")

            watched.shouldBeInstanceOf<TqWatchedConfig>()
        }

        it("should start and stop watching without errors") {
            val configFile = File(tempDir, "config-${UUID.randomUUID()}.properties").also {
                it.writeText("key=value\n")
            }
            val config = TqMapConfig(mapOf("key" to "initial"))
            val watched = TqWatchedConfig(config, configFile.absolutePath)

            watched.startWatching()
            assertTrue(watched.toString().contains("TqWatchedConfig"))
            watched.stopWatching()
        }

        it("should ignore classpath paths gracefully") {
            val config = TqMapConfig(mapOf("key" to "value"))
            val watched = TqWatchedConfig(config, "classpath:app.properties")

            watched.startWatching()
            assertEquals("value", config.get<String>("key"))
            watched.stopWatching()
        }

        it("should handle missing config file") {
            val configFile = File(tempDir, "nonexistent-${UUID.randomUUID()}.properties")
            val config = TqMapConfig(mapOf("key" to "value"))
            val watched = TqWatchedConfig(config, configFile.absolutePath)

            watched.startWatching()
            watched.stopWatching()
        }

        it("should trigger reload when file is modified (integration)") {
            val configFile = File(tempDir, "watch-test-${UUID.randomUUID()}.properties").also {
                it.writeText("""{"database":{"host":"localhost","port":5432}}""")
            }
            val jsonConfig = TqJsonConfig(configFile.absolutePath)
            val watched = TqWatchedConfig(jsonConfig, configFile.absolutePath)

            watched.startWatching()

            val initialDb = jsonConfig.get<Map<*, *>>("database")
            initialDb?.get("host") shouldBe "localhost"

            runBlocking {
                configFile.writeText("""{"database":{"host":"example.com","port":5432}}""")
                delay(2000)
            }

            val reloadedDb = jsonConfig.get<Map<*, *>>("database")
            reloadedDb?.get("host") shouldBe "example.com"

            watched.stopWatching()
        }

        it("should handle file deletion and recreation") {
            val configFile = File(tempDir, "delete-test-${UUID.randomUUID()}.properties").also {
                it.writeText("key=initial\n")
            }
            val config = TqMapConfig(mapOf("key" to "initial"))
            val watched = TqWatchedConfig(config, configFile.absolutePath)

            watched.startWatching()
            configFile.delete()
            runBlocking { delay(500) }
            configFile.writeText("key=updated\n")
            runBlocking { delay(2000) }
            watched.stopWatching()
        }
    }

    describe("TqWatchedConfig lifecycle") {
        it("should allow multiple start/stop cycles") {
            val configFile = File(tempDir, "lifecycle-${UUID.randomUUID()}.properties").also {
                it.writeText("key=value\n")
            }
            val config = TqMapConfig(mapOf("key" to "value"))
            val watched = TqWatchedConfig(config, configFile.absolutePath)

            repeat(3) {
                watched.startWatching()
                watched.stopWatching()
            }
        }

        it("should not affect config when watching classpath") {
            val config = TqMapConfig(mapOf("host" to "original"))
            val watched = TqWatchedConfig(config, "classpath:nonexistent.properties")

            watched.startWatching()
            assertEquals("original", config.get<String>("host"))

            config.reload(mapOf("host" to "changed"))
            assertEquals("changed", config.get<String>("host"))

            watched.stopWatching()
        }
    }
})