package io.tekniq.validation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

private data class PojoTestBean(val id: String, val name: String?, val weight: Float?, val birthday: Date?, val extra: Boolean, val nullable: Boolean?)

class TqValidationTest {
    private lateinit var pojoBased: TqValidation
    private lateinit var mapBased: TqValidation

    @Before fun setup() {
        pojoBased = TqValidation(PojoTestBean("42", "Bob", 140.6f, Date(), true, null))
        mapBased = TqValidation(mapOf(
                "id" to "42",
                "name" to "Bob",
                "weight" to 140.6f,
                "birthday" to Date(),
                "extra" to true,
                "nullable" to null
        ))
    }

    @Test fun and() {
        pojoBased.and("Customized Message") {
            required("name")
            required("monkey", "I'm a purple monkey")
            required("normal")
        }
        assertEquals(0, pojoBased.passed)
        assertEquals("Customized Message", pojoBased.rejections[0].message)
    }

    @Test fun or() {
    }

    @Test fun ifDefinedWithPojo() = ifDefinedBase(pojoBased)
    @Test fun ifDefinedWithMap() = ifDefinedBase(mapBased)
    private fun ifDefinedBase(validation: TqValidation) {
        assertEquals(0, validation.tested)
        assertEquals(0, validation.passed)
        assertEquals(0, validation.rejections.size)
        var triggered = false
        validation.ifDefined("weight") { triggered = true }
        assertTrue(triggered)
        assertEquals(0, validation.tested) // ifDefined and ifNotDefined are not actually validation tests
        assertEquals(0, validation.passed)

        triggered = false
        validation.ifDefined("color") { triggered = true }
        assertFalse(triggered)

        // null is not the same thing as undefined
        triggered = false
        validation.ifDefined("nullable") { triggered = true }
        assertTrue(triggered)
    }

    @Test fun ifNotDefinedWithPojo() = ifNotDefinedBase(pojoBased)
    @Test fun ifNotDefinedWithMap() = ifNotDefinedBase(mapBased)
    private fun ifNotDefinedBase(validation: TqValidation) {
        assertEquals(0, validation.tested)
        assertEquals(0, validation.passed)
        assertEquals(0, validation.rejections.size)
        var triggered = false
        validation.ifNotDefined("weight") { triggered = true }
        assertFalse(triggered)
        assertEquals(0, validation.tested) // ifDefined and ifNotDefined are not actually validation tests
        assertEquals(0, validation.passed)

        triggered = false
        validation.ifNotDefined("color") { triggered = true }
        assertTrue(triggered)

        // null is not the same thing as undefined
        triggered = false
        validation.ifNotDefined("nullable") { triggered = true }
        assertFalse(triggered)
    }

    @Test fun required() {
    }

    @Test fun date() {
    }

    @Test fun numberWithPojo() = numberBase(pojoBased)
    @Test fun numberWithMap() = numberBase(mapBased)

    private fun numberBase(validation: TqValidation) {
        assertEquals(0, validation.tested)
        assertEquals(0, validation.passed)
        assertEquals(0, validation.rejections.size)
        validation.number("weight")
        assertEquals(1, validation.tested)
        assertEquals(1, validation.passed)
        validation.number("name")
        assertEquals(2, validation.tested)
        assertEquals(1, validation.passed)
        assertEquals(1, validation.rejections.size)
    }

    @Test fun string() {
    }

    @Test fun arrayOf() {
    }

    @Test fun length() {
        listOf(pojoBased, mapBased).forEach { validation ->
            assertEquals(0, validation.tested)
            assertEquals(0, validation.passed)
            assertEquals(0, validation.rejections.size)

            validation.length("name")
            assertEquals(1, validation.tested)
            assertEquals(1, validation.passed)
            assertEquals(0, validation.rejections.size)

            validation.length("name", min = 3)
            assertEquals(2, validation.tested)
            assertEquals(2, validation.passed)
            assertEquals(0, validation.rejections.size)

            validation.length("name", min = 5)
            assertEquals(3, validation.tested)
            assertEquals(2, validation.passed)
            assertEquals(1, validation.rejections.size)

            validation.length("name", max = 5)
            assertEquals(4, validation.tested)
            assertEquals(3, validation.passed)
            assertEquals(1, validation.rejections.size)

            validation.length("name", max = 2)
            assertEquals(5, validation.tested)
            assertEquals(3, validation.passed)
            assertEquals(2, validation.rejections.size)

            validation.length("name", max = 2, min = 5)
            assertEquals(6, validation.tested)
            assertEquals(3, validation.passed)
            assertEquals(3, validation.rejections.size)
        }
    }
}