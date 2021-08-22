package io.tekniq.config

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

object TqPropertiesConfigSpek : Spek({
    describe("generic testing") {
        it("works with file loading") {
            val config = try {
                TqPropertiesConfig("src/test/resources/io/tekniq/config/test.properties")
            } catch (e: FileNotFoundException) {
                TqPropertiesConfig("tekniq-core/src/test/resources/io/tekniq/config/test.properties")
            }

            // Shall contain known entries
            assertEquals(3, config.keys.size)
            assertEquals(true, config.contains("name"))
            assertEquals(true, config.contains("age"))
            assertEquals(true, config.contains("cost"))

            // Will not contain unknown entries
            assertEquals(false, config.contains("NAME"))
            assertEquals(false, config.contains("ages"))

            // Correctly converts number types
            assertEquals(42.toShort(), config.getShort("age"))
            assertEquals(42, config.getInt("age"))
            assertEquals(42.toLong(), config.getLong("age"))
            assertEquals(3.14, config.getDouble("cost"))
            assertEquals(3.14.toFloat(), config.getFloat("cost"))
        }

        it("works with classpath loading") {
            val config = TqPropertiesConfig("classpath:/io/tekniq/config/test.properties")
            // Shall contain known entries
            assertEquals(3, config.keys.size)
            assertEquals(true, config.contains("name"))
            assertEquals(true, config.contains("age"))
            assertEquals(true, config.contains("cost"))

            // Will not contain unknown entries
            assertEquals(false, config.contains("NAME"))
            assertEquals(false, config.contains("ages"))

            // Correctly converts number types
            assertEquals(42.toShort(), config.getShort("age"))
            assertEquals(42, config.getInt("age"))
            assertEquals(42.toLong(), config.getLong("age"))
            assertEquals(3.14, config.getDouble("cost"))
            assertEquals(3.14.toFloat(), config.getFloat("cost"))
        }

        it("works with file not found") {
            // Shall be empty with stopOnFailure=false
            val config = TqPropertiesConfig("invalid-folder/test.properties", stopOnFailure = false)
            assertEquals(0, config.keys.size)

            // Shall be throw an exception with default setting
            try {
                TqPropertiesConfig("invalid-folder/test.properties")
                fail("Should not have gotten this far")
            } catch (e: FileNotFoundException) {
                // success
            }
        }

        it("correctly handles special getType conversions like getInt") {
            val config = try {
                TqPropertiesConfig("src/test/resources/io/tekniq/config/test.properties")
            } catch (e: FileNotFoundException) {
                TqPropertiesConfig("tekniq-core/src/test/resources/io/tekniq/config/test.properties")
            }

            assertEquals("42", config.get("age"))
            assertEquals(42, config.getInt("age"))
            assertEquals(42.0, config.getDouble("age"))
            assertNull(config.get("fake"))
            assertNull(config.getInt("fake"))
            assertEquals(3, config.getInt("cost"))
            assertEquals(3.14, config.getDouble("cost"))
            assertEquals("3.14", config.get("cost"))
        }
    }
})
