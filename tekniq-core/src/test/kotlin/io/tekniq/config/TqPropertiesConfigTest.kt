package io.tekniq.config

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.FileNotFoundException

class TqPropertiesConfigTest {
    @Test fun withFileLoading() {
        var config: TqPropertiesConfig
        try {
            config = TqPropertiesConfig("src/test/resources/io/tekniq/config/test.properties")
        } catch(e: FileNotFoundException) {
            config = TqPropertiesConfig("tekniq-core/src/test/resources/io/tekniq/config/test.properties")
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

    @Test fun withClasspathLoading() {
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

    @Test fun withFileNotFound() {
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
}
