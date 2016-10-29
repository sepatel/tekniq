package io.tekniq.web

import java.util.*

data class Rejection(val code: String, val field: String? = null, val message: String? = null)
open class ValidationException(val rejections: Collection<Rejection>, val data: Any? = null) : Exception()

open class Validation(val src: Any?, val path: String = "") {
    val rejections = mutableListOf<Rejection>()
    var tested = 0
    var passed = 0

    fun and(check: Validation.() -> Unit): Validation {
        val validation = Validation(src)
        check(validation)

        if (validation.rejections.size > 0) {
            rejections.add(Rejection(validation.rejections.joinToString {
                if (it.field != null) {
                    return@joinToString "${it.field}.${it.code}"
                }

                it.code
            }, "\$and", path))
        }
        return this
    }

    fun or(check: Validation.() -> Unit): Validation {
        val validation = Validation(src)
        check(validation)

        if (validation.passed == 0) {
            rejections.add(Rejection(validation.rejections.joinToString {
                if (it.field != null) {
                    return@joinToString "${it.field}.${it.code}"
                }

                it.code
            }, "\$or", path))
        }
        return this
    }

    fun merge(vararg validations: Validation): Validation {
        validations.forEach { rejections.addAll(it.rejections) }
        return this
    }

    fun reject(code: String, field: String? = null, message: String? = null): Validation {
        rejections.add(Rejection(code, fieldPath(field), message))
        return this
    }

    fun required(field: String? = null): Validation = test(field, "required") {
        if (it == null) {
            return@test false
        }

        if (it is String && it.trim().length == 0) {
            return@test false
        } else if (it is Collection<*>) {
            return@test it.isNotEmpty()
        }

        true
    }

    fun date(field: String? = null): Validation = test(field, "invalidDate") {
        return@test (it == null || it is Date)
    }

    fun number(field: String? = null): Validation = test(field, "invalidNumber") {
        return@test (it == null || it is Number)
    }

    fun string(field: String? = null): Validation = test(field, "invalidString") {
        return@test (it == null || it is String)
    }

    fun arrayOf(field: String? = null, check: Validation.() -> Unit): Validation = test(field, "invalidArray") {
        if (it !is List<*>) {
            return@test false
        }

        it.forEachIndexed { i, element ->
            var name = fieldPath(field) ?: ""
            if (name.length > 0) {
                name += '.'
            }

            val validation = Validation(element, name + i)
            check(validation)
            rejections.addAll(validation.rejections)
        }
        true
    }

    fun stopOnRejections(data: Any? = null): Validation {
        if (rejections.size > 0) {
            throw ValidationException(rejections, data)
        }
        return this
    }

    open protected fun test(field: String?, code: String, check: (Any?) -> Boolean): Validation {
        if (src == null) {
            rejections.add(Rejection(code, fieldPath(field)))
            return this
        }

        val test = getValue(src, field)
        val validationCheckResult = check(test)
        tested++
        if (validationCheckResult) {
            passed++
        } else {
            rejections.add(Rejection(code, fieldPath(field)))
        }

        return this
    }

    private fun fieldPath(field: String?): String? {
        if (path.length > 0) {
            if (field == null) {
                return path
            }
            return "$path.$field"
        }
        return field
    }

    private fun getValue(src: Any, field: String?): Any? {
        if (field == null) {
            return src
        }
        var value = src
        field.split('.').forEach {
            if (value is Map<*, *>) {
                val key = it
                value.javaClass.getMethod("get", Any::class.java).let {
                    it.isAccessible = true
                    value = it.invoke(value, key) ?: return null
                }
            } else {
                try {
                    value.javaClass.getMethod("get" + it.capitalize()).let {
                        it.isAccessible = true
                        value = it.invoke(value) ?: return null
                    }
                } catch (e: NoSuchMethodException) {
                    return null
                }
            }
        }
        return value
    }
}

abstract class SparklinValidation(src: Any?, path: String = "") : Validation(src, path) {
    abstract fun authz(all: Boolean = false, vararg authz: String): SparklinValidation
}

