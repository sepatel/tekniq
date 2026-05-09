package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object TqPropertiesConfigReloadSpec : DescribeSpec({
    describe("TqPropertiesConfig reload semantics") {
        it("should update values on reload") {
            val config = try {
                TqPropertiesConfig("src/test/resources/io/tekniq/config/test.properties", stopOnFailure = false)
            } catch (e: FileNotFoundException) {
                TqPropertiesConfig("tekniq-core/src/test/resources/io/tekniq/config/test.properties", stopOnFailure = false)
            }

            assertTrue(config.keys.isNotEmpty(), "Config should have keys after loading")
            assertEquals(3, config.keys.size)
            
            config.reload()
            
            assertTrue(config.keys.isNotEmpty(), "Config should have keys after reload")
            assertEquals(3, config.keys.size)
        }

        it("should handle file not found on reload when stopOnFailure=false") {
            val config = TqPropertiesConfig("invalid-folder/test.properties", stopOnFailure = false)

            config.reload()

            assertEquals(0, config.keys.size)
        }
    }
})