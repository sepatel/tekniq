package io.tekniq.config

import kotlin.reflect.KClass

internal object TqConfigTypeCoercion {
    private val truthy = setOf("true", "yes", "1", "on", "y", "t")
    private val falsy = setOf("false", "no", "0", "off", "n", "f")

    fun coerceToBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> when (value.trim().lowercase()) {
                in truthy -> true
                in falsy -> false
                else -> throw TqConfigBindException(
                    message = "Cannot coerce string to Boolean",
                    expectedType = "Boolean",
                    actualValue = value
                )
            }
            is Number -> value.toInt() != 0
            else -> throw TqConfigBindException(
                message = "Cannot coerce to Boolean",
                expectedType = "Boolean",
                actualValue = value
            )
        }
    }

    fun coerceToInt(value: Any?): Int = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Short -> value.toInt()
        is Byte -> value.toInt()
        is Number -> value.toInt()
        is String -> value.trim().toIntOrNull()
            ?: throw TqConfigBindException(
                message = "Cannot parse Int",
                expectedType = "Int",
                actualValue = value
            )
        is Boolean -> if (value) 1 else 0
        else -> throw TqConfigBindException(
            message = "Cannot coerce to Int",
            expectedType = "Int",
            actualValue = value
        )
    }

    fun coerceToLong(value: Any?): Long = when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Short -> value.toLong()
        is Byte -> value.toLong()
        is Number -> value.toLong()
        is String -> value.trim().toLongOrNull()
            ?: throw TqConfigBindException(
                message = "Cannot parse Long",
                expectedType = "Long",
                actualValue = value
            )
        is Boolean -> if (value) 1L else 0L
        else -> throw TqConfigBindException(
            message = "Cannot coerce to Long",
            expectedType = "Long",
            actualValue = value
        )
    }

    fun coerceToShort(value: Any?): Short = when (value) {
        is Short -> value
        is Int -> value.toShort()
        is Long -> value.toShort()
        is Byte -> value.toShort()
        is Number -> value.toShort()
        is String -> value.trim().toShortOrNull()
            ?: throw TqConfigBindException(
                message = "Cannot parse Short",
                expectedType = "Short",
                actualValue = value
            )
        else -> throw TqConfigBindException(
            message = "Cannot coerce to Short",
            expectedType = "Short",
            actualValue = value
        )
    }

    fun coerceToByte(value: Any?): Byte = when (value) {
        is Byte -> value
        is Int -> value.toByte()
        is Long -> value.toByte()
        is Short -> value.toByte()
        is Number -> value.toByte()
        is String -> value.trim().toByteOrNull()
            ?: throw TqConfigBindException(
                message = "Cannot parse Byte",
                expectedType = "Byte",
                actualValue = value
            )
        else -> throw TqConfigBindException(
            message = "Cannot coerce to Byte",
            expectedType = "Byte",
            actualValue = value
        )
    }

    fun coerceToDouble(value: Any?): Double = when (value) {
        is Double -> value
        is Float -> value.toDouble()
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is Short -> value.toDouble()
        is Byte -> value.toDouble()
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
            ?: throw TqConfigBindException(
                message = "Cannot parse Double",
                expectedType = "Double",
                actualValue = value
            )
        is Boolean -> if (value) 1.0 else 0.0
        else -> throw TqConfigBindException(
            message = "Cannot coerce to Double",
            expectedType = "Double",
            actualValue = value
        )
    }

    fun coerceToFloat(value: Any?): Float = when (value) {
        is Float -> value
        is Double -> value.toFloat()
        is Int -> value.toFloat()
        is Long -> value.toFloat()
        is Number -> value.toFloat()
        is String -> value.trim().toFloatOrNull()
            ?: throw TqConfigBindException(
                message = "Cannot parse Float",
                expectedType = "Float",
                actualValue = value
            )
        else -> throw TqConfigBindException(
            message = "Cannot coerce to Float",
            expectedType = "Float",
            actualValue = value
        )
    }

    fun coerceToString(value: Any?): String = when (value) {
        null -> ""
        is String -> value
        else -> value.toString()
    }

    @Suppress("UNCHECKED_CAST")
    fun coerceToEnum(value: Any?, enumClass: KClass<*>): Any {
        val enumConstants = enumClass.java.enumConstants
            ?: throw TqConfigBindException(
                message = "Not an enum class",
                expectedType = enumClass.simpleName
            )
        val str = coerceToString(value).trim()
        val match = enumConstants.firstOrNull {
            (it as Enum<*>).name.equals(str, ignoreCase = true)
        } ?: throw TqConfigBindException(
            message = "Invalid enum value",
            path = null,
            expectedType = enumClass.simpleName + " (one of " + enumConstants.joinToString { (it as Enum<*>).name } + ")",
            actualValue = str
        )
        return match
    }

    fun coerceList(rawList: Iterable<*>, elementClass: KClass<*>): List<Any?> {
        return rawList.map { coerce(it, elementClass) }
    }

    fun coerceCommaSeparated(raw: String, elementClass: KClass<*>): List<Any?> {
        val parts = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return parts.map { coerce(it, elementClass) }
    }

    fun coerce(value: Any?, targetClass: KClass<*>): Any? = when {
        targetClass == String::class -> coerceToString(value)
        targetClass == Int::class -> if (value == null) null else coerceToInt(value)
        targetClass == Long::class -> if (value == null) null else coerceToLong(value)
        targetClass == Short::class -> if (value == null) null else coerceToShort(value)
        targetClass == Byte::class -> if (value == null) null else coerceToByte(value)
        targetClass == Double::class -> if (value == null) null else coerceToDouble(value)
        targetClass == Float::class -> if (value == null) null else coerceToFloat(value)
        targetClass == Boolean::class -> if (value == null) null else coerceToBoolean(value)
        targetClass.java.isEnum -> if (value == null) null else coerceToEnum(value, targetClass)
        else -> throw TqConfigBindException(
            message = "Unsupported scalar type for coercion",
            expectedType = targetClass.simpleName
        )
    }
}
