package io.tekniq.validation

import io.kotest.core.spec.style.DescribeSpec
import java.net.URL
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class ListItem(val id: Int, val text: String)
data class PojoCheckBean(
    val id: String,
    val name: String?,
    val weight: Float?,
    val birthday: Date?,
    val extra: Boolean,
    val nullable: Boolean?,
    val list: List<ListItem> = emptyList(),
    val set: Set<ListItem> = emptySet(),
    val emails: List<String> = emptyList(),
    val fakeInfo: FakeInfo? = null
)

data class FakeInfo(val fake: String, val listing: List<ListItem>, val mappings: Map<String, Any>)

object TqCheckSpec : DescribeSpec({
    val bean = PojoCheckBean(
        "42", "Bob", 140.6f, Date(), true, null,
        list = listOf(ListItem(1, "One"), ListItem(2, "Two")),
        set = setOf(ListItem(3, "Three"), ListItem(4, "Four")),
        emails = listOf("here@example.com", "broken@example..com"),
        fakeInfo = FakeInfo("stuff", emptyList(), mapOf("A" to "Apple"))
    )
    describe("Basic Checking Logic") {
        data class Fake(val named: String)

        lateinit var pojoBased: TqCheck
        lateinit var mapBased: TqCheck

        beforeTest {
            pojoBased = TqCheck(bean)
            mapBased = TqCheck(
                mapOf(
                    "id" to "42",
                    "name" to "Bob",
                    "weight" to 140.6f,
                    "birthday" to Date(),
                    "extra" to true,
                    "nullable" to null,
                    "list" to listOf(ListItem(1, "One"), ListItem(2, "Two")),
                    "set" to setOf(ListItem(3, "Three"), ListItem(4, "Four")),
                    "emails" to listOf("here@example.com", "broken@example..com")
                )
            )
        }

        it("required by field name with map") {
            mapBased.required("name")
            assertTrue(mapBased.reasons.isEmpty())
            mapBased.required("named")
            assertTrue(mapBased.reasons.isNotEmpty())
            println(mapBased.reasons)
        }

        it("required by field name with pojo") {
            pojoBased.required("name")
            assertTrue(pojoBased.reasons.isEmpty())
            pojoBased.required("named")
            assertTrue(pojoBased.reasons.isNotEmpty())
            println(pojoBased.reasons)
        }

        it("required by property") {
            pojoBased.required(PojoCheckBean::name)
            assertTrue(pojoBased.reasons.isEmpty())
            pojoBased.required(Fake::named)
            assertTrue(pojoBased.reasons.isNotEmpty())
            println(pojoBased.reasons)
        }

        it("requiredIfDefined") {
            pojoBased.required(PojoCheckBean::name, ifDefined = true)
            assertTrue(pojoBased.reasons.isEmpty())
            pojoBased.required(Fake::named, ifDefined = true)
            assertTrue(pojoBased.reasons.isEmpty())
        }

        it("listOf checks by pojo") {
            pojoBased.listOf(PojoCheckBean::list) {
                number("id")
                string("text")
            }
            assertTrue(pojoBased.reasons.isEmpty())
        }

        it("listOf undefined field") {
            pojoBased.listOf("listed") {
                number("id")
                string("text")
            }
            assertTrue(pojoBased.reasons.isNotEmpty())
        }
        it("listOf undefined field w/o required") {
            pojoBased.listOf("listed", ifDefined = true) {
                number("id")
                string("text")
            }
            assertTrue(pojoBased.reasons.isEmpty())
        }
    }

    describe("AND logic conditions") {
        lateinit var check: TqCheck

        beforeTest {
            check = TqCheck(bean)
        }

        it("works with all conditions passing") {
            check.and {
                required(PojoCheckBean::id)
                required(PojoCheckBean::name)
                required(PojoCheckBean::emails)
            }
            assertTrue(check.reasons.isEmpty())
        }

        it("works with all conditions failing") {
            check.and {
                required(FakeInfo::fake)
                required("anotherFake")
                required(FakeInfo::listing)
            }
            assertTrue(check.reasons.isNotEmpty())
        }

        it("works with first condition failing") {
            check.and {
                required(FakeInfo::fake)
                required(PojoCheckBean::name)
                required(PojoCheckBean::emails)
            }
            assertTrue(check.reasons.isNotEmpty())
        }

        it("works with last condition failing") {
            check.and {
                required(PojoCheckBean::id)
                required(PojoCheckBean::name)
                required(FakeInfo::mappings)
            }
            assertTrue(check.reasons.isNotEmpty())
        }
    }

    describe("OR logic conditions") {
        lateinit var check: TqCheck

        beforeTest {
            check = TqCheck(bean)
        }

        it("works with all conditions passing") {
            check.or {
                required(PojoCheckBean::id)
                required(PojoCheckBean::name)
                required(PojoCheckBean::emails)
            }
            assertTrue(check.reasons.isEmpty())
        }

        it("works with all conditions failing") {
            check.or {
                required(FakeInfo::fake)
                required("anotherFake")
                required(FakeInfo::listing)
            }
            assertTrue(check.reasons.isNotEmpty())
        }

        it("works with first condition failing") {
            check.or {
                required(FakeInfo::fake)
                required(PojoCheckBean::name)
                required(PojoCheckBean::emails)
            }
            assertTrue(check.reasons.isEmpty())
        }

        it("works with last condition failing") {
            check.or {
                required(PojoCheckBean::id)
                required(PojoCheckBean::name)
                required(FakeInfo::mappings)
            }
            assertTrue(check.reasons.isEmpty())
        }
    }

    describe("Default Message Translation") {
        it("Shall translate templated simple messages elegantly") {
            val reasons = TqCheck(bean)
                .required("purple", "Call me {{name}}")
                .reasons
            assertTrue { reasons.isNotEmpty() }
            assertEquals("Call me Bob", reasons.first().message)
        }

        it("Supports dot notation drill downs") {
            assertEquals(
                "I like stuff", TqCheck(bean)
                    .required("purple", "I like {{fakeInfo.fake}}")
                    .reasons.first().message
            )

            assertEquals(
                "Favorite fruit is Apple", TqCheck(bean)
                    .required("purple", "Favorite fruit is {{fakeInfo.mappings.A}}")
                    .reasons.first().message
            )
        }

        it("Shall translate templated simple lists as well") {
            assertEquals(
                "Text me at [here@example.com, broken@example..com]", TqCheck(bean)
                    .required("purple", "Text me at {{emails}}")
                    .reasons.first().message
            )
        }

        xit("Shall translate templated simple items in a lists") {
            assertEquals(
                "Mine is here@example.com", TqCheck(bean)
                    .required("purple", "Mine is {{emails[0]}}")
                    .reasons.first().message
            )
        }
    }

    describe("URL Verification") {
        data class UrlContainingBean(val name: String, val website: String, val url: URL)
        it("Shall verify the happy path case") {
            val good = UrlContainingBean("Good", "https://www.google.com", URL("https://www.google.com"))
            val reasons = TqCheck(good)
                .url(UrlContainingBean::website)
                .url(UrlContainingBean::url)
                .reasons
            assertTrue(reasons.isEmpty())
        }

        it("Shall identify invalid string urls") {
            val badWebsite = UrlContainingBean("Good", "www.google.com", URL("https://www.google.com"))
            val reasons = TqCheck(badWebsite)
                .url(UrlContainingBean::website)
                .url(UrlContainingBean::url)
                .reasons
            assertTrue(reasons.isNotEmpty())
            assertEquals(1, reasons.size)
            assertEquals("InvalidURL website", reasons.first().code)
        }
    }

    describe("notBlank validation") {
        it("should fail with null value") {
            val check = TqCheck(mapOf("field" to null as String?))
            check.notBlank("field")
            assertTrue(check.reasons.isNotEmpty())
            assertEquals("Blank field", check.reasons.first().code)
        }

        it("should fail with empty string") {
            val check = TqCheck(mapOf("field" to ""))
            check.notBlank("field")
            assertTrue(check.reasons.isNotEmpty())
        }

        it("should fail with whitespace-only string") {
            val check = TqCheck(mapOf("field" to "   "))
            check.notBlank("field")
            assertTrue(check.reasons.isNotEmpty())
        }

        it("should pass with valid string") {
            val check = TqCheck(mapOf("field" to "hello"))
            check.notBlank("field")
            assertTrue(check.reasons.isEmpty())
        }

        it("should pass with valid string containing spaces") {
            val check = TqCheck(mapOf("field" to "hello world"))
            check.notBlank("field")
            assertTrue(check.reasons.isEmpty())
        }

        it("should handle ifDefined - skip when undefined") {
            val check = TqCheck(mapOf("other" to "value"))
            check.notBlank("field", ifDefined = true)
            assertTrue(check.reasons.isEmpty())
        }

        it("should work on lists - pass when list is not empty") {
            val check = TqCheck(mapOf("field" to listOf("a", "b")))
            check.notBlank("field")
            assertTrue(check.reasons.isEmpty())
        }

        it("should work on lists - fail when list is empty") {
            val check = TqCheck(mapOf("field" to emptyList<String>()))
            check.notBlank("field")
            assertTrue(check.reasons.isNotEmpty())
        }

        it("should work on numbers - fail when null") {
            val check = TqCheck(mapOf("field" to null as Int?))
            check.notBlank("field")
            assertTrue(check.reasons.isNotEmpty())
        }

        it("should work on numbers - pass when value is zero") {
            val check = TqCheck(mapOf("field" to 0))
            check.notBlank("field")
            assertTrue(check.reasons.isEmpty())
        }

        it("should pass when field is present") {
            val check = TqCheck(mapOf("field" to "value"))
            check.notBlank("field")
            assertTrue(check.reasons.isEmpty())
        }
    }

    describe("custom constraint") {
        it("should allow custom validation predicate") {
            val check = TqCheck(mapOf("value" to 42))
            check.custom("Even", field = "value") { ((it as? Int) ?: 0) % 2 == 0 }
            assertTrue(check.reasons.isEmpty())
        }

        it("should fail custom validation when predicate returns false") {
            val check = TqCheck(mapOf("value" to -5))
            check.custom("Positive", field = "value") { ((it as? Int) ?: 0) > 0 }
            assertTrue(check.reasons.isNotEmpty())
            assertEquals("Invalid+Positive value", check.reasons.first().code)
        }

        it("should work with custom name") {
            val check = TqCheck(mapOf("name" to "Bob"))
            check.custom("Capitalized", field = "name") { (it as? String)?.first()?.isUpperCase() == true }
            assertTrue(check.reasons.isEmpty())
        }

        it("should respect ifDefined parameter") {
            val check = TqCheck(mapOf("other" to "value"))
            check.custom("Required", field = "field", ifDefined = true) { it != null }
            assertTrue(check.reasons.isEmpty())
        }

        it("should handle non-string types") {
            val check = TqCheck(mapOf("field" to 123))
            check.custom("NonZero", field = "field") { (it as? Int) != 0 }
            assertTrue(check.reasons.isEmpty())
        }
    }
})

