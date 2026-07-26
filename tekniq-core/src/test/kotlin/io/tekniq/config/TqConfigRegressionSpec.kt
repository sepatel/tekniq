package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/** Regression coverage for the config defects fixed in 0.23.0. */
object TqConfigRegressionSpec : DescribeSpec({
    describe("concurrent reads") {
        // Reads memoise into a map shared by all callers. While that map was an unguarded
        // LinkedHashMap, concurrent reads corrupted its table: it grew past the number of distinct
        // keys and handed back values belonging to other keys.
        it("never returns a value belonging to another key") {
            val expected = (1..2000).associate { "key$it" to it }
            val config = LazyConfig(expected)
            val wrong = ConcurrentLinkedQueue<String>()

            val threads = List(8) {
                Thread {
                    expected.forEach { (key, value) ->
                        val actual = config.get<Int>(key)
                        if (actual != value) wrong += "$key expected $value got $actual"
                    }
                }
            }
            threads.forEach(Thread::start)
            threads.forEach(Thread::join)

            wrong.shouldBeEmpty()
            config.keys.size shouldBe expected.size
        }
    }

    describe("contains") {
        // contains() called getValue and threw the result away, so it reported false for any key
        // that had not already been read through get().
        it("reports a key the implementation can supply but nothing has read yet") {
            LazyConfig(mapOf("fresh" to "value")).contains("fresh") shouldBe true
        }

        it("still reports false for an unknown key") {
            LazyConfig(mapOf("fresh" to "value")).contains("absent") shouldBe false
        }
    }

    describe("reload") {
        it("removes keys that are absent from the new values") {
            val config = TqMapConfig(mapOf("a" to 1, "b" to 2))
            config.reload(mapOf("a" to 9))

            config.get<Int>("a") shouldBe 9
            config.get<Int>("b") shouldBe null
            config.keys shouldBe setOf("a")
        }
    }

    describe("TqPropertiesConfig failed reload") {
        // A failed load returned an empty map, and reload treats absent keys as removals, so simply
        // saving a half-written file made every key disappear.
        it("keeps the previous values when the file becomes unreadable") {
            val file = File.createTempFile("tekniq", ".properties").apply {
                writeText("a=1\nb=2\n")
                deleteOnExit()
            }
            val config = TqPropertiesConfig(file.absolutePath, stopOnFailure = false)
            config.get<String>("a") shouldBe "1"

            file.delete()
            config.reload()

            config.get<String>("a") shouldBe "1"
            config.keys shouldContain "b"
        }

        it("picks up new values when the file is readable again") {
            val file = File.createTempFile("tekniq", ".properties").apply {
                writeText("a=1\n")
                deleteOnExit()
            }
            val config = TqPropertiesConfig(file.absolutePath, stopOnFailure = false)

            file.writeText("a=2\n")
            config.reload()

            config.get<String>("a") shouldBe "2"
        }
    }
})

/**
 * Supplies values lazily rather than pre-loading them, which is what a config over an environment or
 * a remote store does -- and the only shape that exercises the memoisation path.
 */
private class LazyConfig(private val source: Map<String, Any>) : TqConfig() {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(key: String, type: Class<T>?): T? = source[key] as T?
}
