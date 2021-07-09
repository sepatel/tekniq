package io.tekniq.glob

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private enum class TestMatchingType { insensitive, globstar, contains, extended }
object TqGlobSpek : Spek({
    fun test(pattern: String, match: String, type: TestMatchingType, defaultResult: Boolean, flaggedResult: Boolean) {
        val result = when (type) {
            TestMatchingType.insensitive -> Pair(
                TqGlob.toRegEx(pattern).matches(match),
                TqGlob.toRegEx(pattern, insensitive = true).matches(match),
            )
            TestMatchingType.globstar -> Pair(
                TqGlob.toRegEx(pattern).matches(match),
                TqGlob.toRegEx(pattern, globstar = false).matches(match),
            )
            TestMatchingType.contains -> Pair(
                TqGlob.toRegEx(pattern).matches(match),
                TqGlob.toRegEx(pattern, contains = true).matches(match),
            )
            TestMatchingType.extended -> Pair(
                TqGlob.toRegEx(pattern).matches(match),
                TqGlob.toRegEx(pattern, extended = false).matches(match),
            )
        }

        assertTrue("[Default $type] $pattern does not match $match") { result.first == defaultResult }
        assertTrue("[Toggled $type] $pattern does not match $match") { result.second == flaggedResult }
    }

    describe("Contains Matching") {
        it("Match Everything") {
            test("*", "foo", TestMatchingType.contains, true, true)
        }

        it("Match the end") {
            test("f*", "foo", TestMatchingType.contains, true, true)
        }

        it("Match the start") {
            test("*o", "foo", TestMatchingType.contains, true, true)
        }

        it("Match the middle") {
            test("fi*uck", "firetruck", TestMatchingType.contains, true, true)
            test("uc", "firetruck", TestMatchingType.contains, false, true)
        }

        it("Match nothing") {
            test("fire*truck", "firetruck", TestMatchingType.contains, true, true)
        }
    }

    describe("Globstar Matching") {
        it("Common star Patterns") {
            test("*.min.js", "http://example.com/js/jquery.min.js", TestMatchingType.globstar, false, true)
            test(
                "http://example.com/*/jquery.min.js",
                "http://example.com/js/jquery.min.js",
                TestMatchingType.globstar,
                true,
                true
            )
            test(
                "http://example.com/js/*.min.js",
                "http://example.com/js/jquery.min.js",
                TestMatchingType.globstar,
                true,
                true
            )
            test("*.min.*", "http://example.com/js/jquery.min.js", TestMatchingType.globstar, false, true)
            test("*/js/*.js", "http://example.com/js/jquery.min.js", TestMatchingType.globstar, false, true)
        }

        it("Common globstar patterns") {
            test("**/*.min.js", "http://example.com/js/jquery.min.js", TestMatchingType.globstar, true, true)
            test("/example/com/js/*jq*.js", "/example/com/js/jquery.min.js", TestMatchingType.globstar, true, true)
            test("/example/*/js/*jq*.js", "/example/com/js/jquery.min.js", TestMatchingType.globstar, true, true)
            test("/example/**/*jq*.js", "/example/com/js/jquery.min.js", TestMatchingType.globstar, true, true)
        }
    }

    describe("Character Matching") {
        it("Single Character") {
            test("f?o", "fooo", TestMatchingType.extended, false, false)
            test("f?oo", "fooo", TestMatchingType.extended, true, false)
            test("f?o?", "fooo", TestMatchingType.extended, true, false)
            test("?fo", "fooo", TestMatchingType.extended, false, false)
            test("f?oo", "foo", TestMatchingType.extended, false, false)
            test("foo?", "foo", TestMatchingType.extended, false, false)
        }

        it("Character Range") {
            test("fo[oz]", "foo", TestMatchingType.extended, true, false)
            test("fo[oz]", "foz", TestMatchingType.extended, true, false)
            test("fo[oz]", "fog", TestMatchingType.extended, false, false)
        }

        it("Character Grouping") {
//            test("foo{bar,baz}", "foobar", TestMatchingType.extended, true, false)
            test("foo{bar,baz}", "foobaz", TestMatchingType.extended, true, false)
//            test("foo{bar,baz}", "foobaaz", TestMatchingType.extended, false, false)
//            test("foo{bar,b*z}", "foobaaz", TestMatchingType.extended, true, false)
        }

        it("Complex Matches") {
            assertTrue {
                TqGlob.toRegEx("http://?o[oz].b*z.com/{*.js,*.html}")
                    .matches("http://foo.baaz.com/jquery.min.js")
            }
            assertFalse {
                TqGlob.toRegEx("?o[oz].b*z.com/{*.js,*.html}")
                    .matches("http://foo.baaz.com/jquery.min.js")
            }
            assertFalse {
                TqGlob.toRegEx("http://?o[oz].b*z.com/{*.js,*.html}")
                    .matches("http://moz.buzz.com/index.htm")
            }
            assertTrue {
                TqGlob.toRegEx("http://?o[oz].b*z.com/{*.js,*.html}", contains = true)
                    .matches("http://foo.baaz.com/jquery.min.js")
            }
            assertTrue {
                TqGlob.toRegEx("?o[oz].b*z.com/{*.js,*.html}", contains = true)
                    .matches("http://foo.baaz.com/jquery.min.js")
            }
        }
    }
})
