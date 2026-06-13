package io.tekniq.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

data class SimpleConfig(
    val name: String,
    val port: Int,
    val enabled: Boolean = true
)

data class DefaultsConfig(
    val a: String = "alpha",
    val b: Int = 42,
    val c: Boolean = false
)

@TqConfigPrefix("server")
data class AnnotatedConfig(
    val host: String,
    val port: Int = 8080
)

data class NullableFields(
    val name: String?,
    val count: Int?
)

data class DbConfig(
    val host: String,
    val port: Int = 5432
)

object TqConfigBindSpec : DescribeSpec({
    describe("bind<T>() basics") {
        it("binds a data class with all required fields") {
            val config = TqMapConfig(mapOf("name" to "alice", "port" to 8080))
            val bound = config.bind<SimpleConfig>()
            bound shouldBe SimpleConfig(name = "alice", port = 8080, enabled = true)
        }

        it("uses Kotlin default values when keys are missing") {
            val config = TqMapConfig(emptyMap())
            val bound = config.bind<DefaultsConfig>()
            bound shouldBe DefaultsConfig()
        }

        it("binds a data class where every field has a default") {
            val config = TqMapConfig(emptyMap())
            val bound = config.bind<DefaultsConfig>()
            bound shouldBe DefaultsConfig()
        }

        it("binds nullable fields to null when keys are missing") {
            val config = TqMapConfig(emptyMap())
            val bound = config.bind<NullableFields>()
            bound shouldBe NullableFields(name = null, count = null)
        }

        it("binds nullable fields to provided values when keys are present") {
            val config = TqMapConfig(mapOf("name" to "x", "count" to 7))
            val bound = config.bind<NullableFields>()
            bound shouldBe NullableFields(name = "x", count = 7)
        }
    }

    describe("prefix handling") {
        it("uses @TqConfigPrefix annotation to scope all keys") {
            val config = TqMapConfig(mapOf("server.host" to "localhost", "server.port" to 9090))
            val bound = config.bind<AnnotatedConfig>()
            bound shouldBe AnnotatedConfig(host = "localhost", port = 9090)
        }

        it("accepts an explicit prefix argument") {
            val config = TqMapConfig(mapOf("db.host" to "x", "db.port" to 5432))
            val bound = config.bind<DbConfig>("db")
            bound shouldBe DbConfig(host = "x", port = 5432)
        }

        it("annotation wins when both annotation and explicit prefix are provided") {
            val config = TqMapConfig(mapOf("server.host" to "x", "server.port" to 9090))
            val bound = config.bind<AnnotatedConfig>("ignored")
            bound shouldBe AnnotatedConfig(host = "x", port = 9090)
        }

        it("binds without any prefix when class has no annotation and no arg given") {
            val config = TqMapConfig(mapOf("name" to "x", "port" to 1))
            val bound = config.bind<SimpleConfig>()
            bound shouldBe SimpleConfig("x", 1, true)
        }
    }

    describe("error handling") {
        it("throws TqConfigBindException with path when a required field is missing") {
            val config = TqMapConfig(mapOf("name" to "alice"))
            val ex = shouldThrow<TqConfigBindException> { config.bind<SimpleConfig>() }
            ex.path shouldBe "port"
        }

        it("throws when value cannot be coerced to target type") {
            val config = TqMapConfig(mapOf("name" to "alice", "port" to "not-a-number"))
            val ex = shouldThrow<TqConfigBindException> { config.bind<SimpleConfig>() }
            ex.path shouldBe "port"
            ex.expectedType shouldContain "Int"
        }
    }

    describe("re-binding") {
        it("re-binds cleanly after a reload with new values") {
            val config = TqMapConfig(mapOf("name" to "alice", "port" to 1))
            val first = config.bind<SimpleConfig>()
            first.port shouldBe 1

            config.reloadWithMap(mapOf("name" to "alice", "port" to 999))
            val second = config.bind<SimpleConfig>()
            second.port shouldBe 999
        }
    }
})
