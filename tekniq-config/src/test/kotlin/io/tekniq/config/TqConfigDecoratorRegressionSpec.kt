package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/** Regression coverage for the decorator and file-loader defects fixed in 0.23.0. */
object TqConfigDecoratorRegressionSpec : DescribeSpec({
    describe("TqSynchronizedConfig staleness") {
        // The wrapper inherited its own memoisation cache and reload only delegated downward, so any
        // key read before a reload was served from the wrapper's stale copy forever. The existing
        // spec only read after reloading, which is why it passed.
        it("sees a new value for a key that was read before the reload") {
            val config = TqSynchronizedConfig(TqMapConfig(mapOf("key" to "value")))

            config.get<String>("key") shouldBe "value"
            config.reload(mapOf("key" to "newValue"))

            config.get<String>("key") shouldBe "newValue"
        }

        it("sees a reload performed directly on the delegate") {
            val delegate = TqMapConfig(mapOf("key" to "value"))
            val config = TqSynchronizedConfig(delegate)

            config.get<String>("key") shouldBe "value"
            delegate.reload(mapOf("key" to "newValue"))

            config.get<String>("key") shouldBe "newValue"
        }

        it("drops keys removed by a reload") {
            val config = TqSynchronizedConfig(TqMapConfig(mapOf("a" to 1, "b" to 2)))

            config.get<Int>("b") shouldBe 2
            config.reload(mapOf("a" to 1))

            config.get<Int>("b") shouldBe null
            config.keys shouldBe setOf("a")
        }
    }

    describe("TqSynchronizedConfig concurrency") {
        it("never returns a value belonging to another key") {
            val expected = (1..2000).associate { "key$it" to it }
            val config = TqSynchronizedConfig(LazyDelegate(expected))
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
        }
    }

    describe("failed reload preserves configuration") {
        // Loaders returned an empty map on failure and reload treats absent keys as removals, so a
        // transiently unreadable file wiped every value out of a running application.
        it("keeps previous values for json") {
            val file = tempFile(".json", """{"a":1,"b":2}""")
            val config = TqJsonConfig(file.absolutePath, stopOnFailure = false)
            config.get<Int>("a") shouldBe 1

            file.writeText("{ this is not json")
            config.reload()

            config.get<Int>("a") shouldBe 1
            config.keys shouldContain "b"
        }

        it("keeps previous values for yaml") {
            val file = tempFile(".yaml", "a: 1\nb: 2\n")
            val config = TqYamlConfig(file.absolutePath, stopOnFailure = false)
            config.get<Int>("a") shouldBe 1

            file.writeText("a: [unclosed\n  bad: indent")
            config.reload()

            config.get<Int>("a") shouldBe 1
            config.keys shouldContain "b"
        }

        it("still applies a reload that succeeds") {
            val file = tempFile(".json", """{"a":1}""")
            val config = TqJsonConfig(file.absolutePath, stopOnFailure = false)

            file.writeText("""{"a":2}""")
            config.reload()

            config.get<Int>("a") shouldBe 2
        }

        it("treats an empty yaml document as an empty config rather than an error") {
            val file = tempFile(".yaml", "")
            TqYamlConfig(file.absolutePath).keys.shouldBeEmpty()
        }
    }
})

private fun tempFile(suffix: String, content: String): File =
    File.createTempFile("tekniq", suffix).apply {
        writeText(content)
        deleteOnExit()
    }

private class LazyDelegate(private val source: Map<String, Any>) : TqConfig() {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(key: String, type: Class<T>?): T? = source[key] as T?
}
