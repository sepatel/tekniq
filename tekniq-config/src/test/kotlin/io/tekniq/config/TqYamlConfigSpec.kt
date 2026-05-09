package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object TqYamlConfigSpec : DescribeSpec({
    val tempDir = File(System.getProperty("java.io.tmpdir"), "tekniq-yaml-test-${System.currentTimeMillis()}").also { it.mkdirs() }
    val testYaml = File(tempDir, "test.yaml").also {
        it.writeText("""
database:
  host: localhost
  port: 5432
app:
  name: tekniq-test
  version: 1.0.0
        """.trimIndent())
    }

    afterSpec { tempDir.deleteRecursively() }

    describe("TqYamlConfig parsing") {
        it("should load YAML from file path") {
            val config = TqYamlConfig(testYaml.absolutePath)

            val db = config.get<Map<*, *>>("database")
            db.shouldBeInstanceOf<Map<*, *>>()
            db?.get("host") shouldBe "localhost"
            db?.get("port") shouldBe 5432

            val app = config.get<Map<*, *>>("app")
            app?.get("name") shouldBe "tekniq-test"
        }

        it("should return null for missing keys") {
            val config = TqYamlConfig(testYaml.absolutePath)

            assertNull(config.get<String>("nonexistent"))
        }

        it("should check key existence") {
            val config = TqYamlConfig(testYaml.absolutePath)

            assertTrue(config.contains("database"))
            assertTrue(config.contains("app"))
            assertFalse(config.contains("nonexistent"))
        }

        it("should handle type conversion") {
            val config = TqYamlConfig(testYaml.absolutePath)

            val db = config.get<Map<*, *>>("database")
            val port = db?.get("port")
            port shouldBe 5432
        }
    }

    describe("TqYamlConfig reload") {
        it("should preserve values on reload from file") {
            val config = TqYamlConfig(testYaml.absolutePath)

            config.get<Map<*, *>>("database")?.get("host") shouldBe "localhost"
            config.reload()
            config.get<Map<*, *>>("database")?.get("host") shouldBe "localhost"
        }

        it("should reload with new values") {
            val config = TqYamlConfig(testYaml.absolutePath)

            config.get<Map<*, *>>("database")?.get("host") shouldBe "localhost"
            config.reload(mapOf("database" to mapOf("host" to "example.com")))
            config.get<Map<*, *>>("database")?.get("host") shouldBe "example.com"
        }
    }

    describe("TqYamlConfig stopOnFailure handling") {
        it("should return empty map when file not found and stopOnFailure=false") {
            val config = TqYamlConfig("nonexistent/path/file.yaml", stopOnFailure = false)

            assertEquals(0, config.keys.size)
            assertNull(config.get<String>("any.key"))
        }

        it("should throw exception when file not found and stopOnFailure=true") {
            var thrown = false
            try {
                TqYamlConfig("nonexistent/path/file.yaml", stopOnFailure = true)
            } catch (e: Exception) {
                thrown = true
            }
            assertTrue(thrown)
        }

        it("should handle reload failure gracefully when stopOnFailure=false") {
            val config = TqYamlConfig("nonexistent/path/file.yaml", stopOnFailure = false)
            config.reload()
            assertEquals(0, config.keys.size)
        }
    }

    describe("TqYamlConfig extends TqMapConfig") {
        it("should be instance of TqMapConfig") {
            val config = TqYamlConfig(testYaml.absolutePath)
            config.shouldBeInstanceOf<TqMapConfig>()
        }
    }
})