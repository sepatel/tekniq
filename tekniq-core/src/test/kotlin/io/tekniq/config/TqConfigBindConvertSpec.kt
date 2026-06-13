package io.tekniq.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

enum class Status { ACTIVE, INACTIVE, PENDING }

data class AllNumericTypes(
    val i: Int,
    val l: Long,
    val s: Short,
    val b: Byte,
    val d: Double,
    val f: Float,
    val bool: Boolean,
    val str: String
)

data class StringCoercion(
    val name: String,
    val count: Int,
    val ratio: Double,
    val flag: Boolean
)

data class EnumConfig(
    val status: Status
)

data class CoercedBool(
    val flag: Boolean
)

object TqConfigBindConvertSpec : DescribeSpec({
    describe("numeric coercion") {
        it("coerces string-typed numeric values to Int/Long/Double/Short/Byte") {
            val config = TqMapConfig(mapOf(
                "i" to "42",
                "l" to "10000000000",
                "s" to "7",
                "b" to "1",
                "d" to "3.14",
                "f" to "2.5",
                "bool" to "true",
                "str" to "hello"
            ))
            val bound = config.bind<AllNumericTypes>()
            bound shouldBe AllNumericTypes(42, 10000000000L, 7, 1, 3.14, 2.5f, true, "hello")
        }

        it("coerces numbers of different width to target type") {
            val config = TqMapConfig(mapOf(
                "i" to 42L,
                "l" to 7,
                "s" to 5,
                "b" to 3,
                "d" to 1,
                "f" to 2.0,
                "bool" to false,
                "str" to "x"
            ))
            val bound = config.bind<AllNumericTypes>()
            bound shouldBe AllNumericTypes(42, 7L, 5, 3, 1.0, 2.0f, false, "x")
        }

        it("throws with clear message when string cannot parse to Int") {
            val config = TqMapConfig(mapOf("name" to "x", "count" to "not-a-number"))
            val ex = shouldThrow<TqConfigBindException> { config.bind<StringCoercion>() }
            ex.path shouldBe "count"
            ex.message shouldContain "Int"
        }
    }

    describe("Boolean coercion") {
        it("accepts true/false case-insensitively") {
            for (v in listOf("true", "TRUE", "True", "false", "FALSE", "False")) {
                val config = TqMapConfig(mapOf("flag" to v))
                val bound = config.bind<CoercedBool>()
                bound.flag shouldBe (v.lowercase() == "true")
            }
        }

        it("accepts yes/no on/off 1/0 t/f y/n case-insensitively") {
            for ((input, expected) in listOf(
                "yes" to true, "YES" to true, "no" to false, "NO" to false,
                "on" to true, "off" to false, "1" to true, "0" to false,
                "t" to true, "f" to false, "y" to true, "n" to false
            )) {
                val config = TqMapConfig(mapOf("flag" to input))
                val bound = config.bind<CoercedBool>()
                bound.flag shouldBe expected
            }
        }

        it("accepts native Boolean values directly") {
            val config = TqMapConfig(mapOf("flag" to true))
            val bound = config.bind<CoercedBool>()
            bound.flag shouldBe true
        }

        it("throws on unparseable string") {
            val config = TqMapConfig(mapOf("flag" to "maybe"))
            val ex = shouldThrow<TqConfigBindException> { config.bind<CoercedBool>() }
            ex.expectedType shouldContain "Boolean"
        }
    }

    describe("enum coercion") {
        it("binds string to enum value case-insensitively") {
            val config = TqMapConfig(mapOf("status" to "active"))
            val bound = config.bind<EnumConfig>()
            bound.status shouldBe Status.ACTIVE
        }

        it("binds with mixed case") {
            val config = TqMapConfig(mapOf("status" to "PeNdInG"))
            val bound = config.bind<EnumConfig>()
            bound.status shouldBe Status.PENDING
        }

        it("throws and lists valid values on bad enum") {
            val config = TqMapConfig(mapOf("status" to "unknown"))
            val ex = shouldThrow<TqConfigBindException> { config.bind<EnumConfig>() }
            ex.expectedType shouldContain "ACTIVE"
            ex.expectedType shouldContain "INACTIVE"
        }
    }
})
