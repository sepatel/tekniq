package io.tekniq.validation

import java.util.*
import java.util.regex.Pattern

private val emailPattern =
    Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:[a-z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\\b")

@Deprecated("Replaced with DeniedReason")
data class Rejection(val code: String, val field: String? = null, val message: String? = null)

@Deprecated("Replaced with CheckException")
open class ValidationException(val rejections: Collection<Rejection>, val data: Any? = null) : Exception() {
    override val message: String?
        get() = rejections.joinToString {
            when {
                it.message != null -> it.message
                it.field != null -> "${it.code}.${it.field}"
                else -> it.code
            }
        }
}

@Deprecated("Replaced with TqCheck")
open class TqValidation(val src: Any? = null, val path: String = "") {
    val rejections = mutableListOf<Rejection>()
    var tested = 0
    var passed = 0

    fun and(message: String? = null, check: TqValidation.() -> Unit): TqValidation {
        val validation = TqValidation(src)
        check(validation)

        if (validation.rejections.size > 0) {
            rejections.add(Rejection(validation.rejections.joinToString {
                if (it.field != null) {
                    return@joinToString "${it.field}.${it.code}"
                }

                it.code
            }, "\$and", message))
        }
        return this
    }

    fun or(message: String? = null, check: TqValidation.() -> Unit): TqValidation {
        val validation = TqValidation(src)
        check(validation)

        if (validation.passed == 0) {
            rejections.add(Rejection(validation.rejections.joinToString {
                if (it.field != null) {
                    return@joinToString "${it.field}.${it.code}"
                }

                it.code
            }, "\$or", message))
        }
        return this
    }

    fun merge(vararg validations: TqValidation): TqValidation {
        validations.forEach {
            rejections.addAll(it.rejections)
            tested += it.tested
            passed += it.passed
        }
        return this
    }

    fun reject(code: String, field: String? = null, message: String? = null): TqValidation {
        rejections.add(Rejection(code, fieldPath(field), message))
        return this
    }

    fun ifDefined(field: String, action: () -> Unit): TqValidation {
        if (src == null) {
            return this
        }

        var value = src
        field.split('.').forEach {
            when (value) {
                null -> return this
                is Map<*, *> -> {
                    if (!(value as Map<*, *>).containsKey(it)) {
                        return this
                    }
                    value = (value as Map<*, *>)[it]
                }
                else -> try {
                    value!!.javaClass.getMethod("get" + it.capitalize()).let { method ->
                        method.isAccessible = true
                        value = method.invoke(value)
                    }
                } catch (e: NoSuchMethodException) {
                    return this
                }
            }
        }

        action.invoke()
        return this
    }

    // TODO: Is there a cleaner way to reverse the ifDefined logic for this implementation instead?
    fun ifNotDefined(field: String, action: () -> Unit): TqValidation {
        if (src == null) {
            action.invoke()
            return this
        }

        var value = src
        field.split('.').forEach {
            when (value) {
                null -> {
                    action.invoke()
                    return this
                }
                is Map<*, *> -> {
                    if (!(value as Map<*, *>).containsKey(it)) {
                        action.invoke()
                        return this
                    }
                    value = (value as Map<*, *>)[it]
                }
                else -> try {
                    value!!.javaClass.getMethod("get" + it.capitalize()).let { method ->
                        method.isAccessible = true
                        value = method.invoke(value)
                    }
                } catch (e: NoSuchMethodException) {
                    action.invoke()
                    return this
                }
            }
        }

        return this
    }

    fun required(field: String? = null, message: String? = null): TqValidation = test(field, "required", message) {
        if (it == null) {
            return@test false
        }

        if (it is String && it.trim().isEmpty()) {
            return@test false
        } else if (it is Collection<*>) {
            return@test it.isNotEmpty()
        }

        true
    }

    fun requiredOrNull(field: String? = null, message: String? = null): TqValidation =
        test(field, "required", message) {
            if (it is String && it.trim().isEmpty()) {
                return@test false
            } else if (it is Collection<*>) {
                return@test it.isNotEmpty()
            }

            true
        }

    fun date(field: String? = null, message: String? = null): TqValidation = test(field, "invalidDate", message) {
        return@test (it == null || it is Date)
    }

    fun email(field: String? = null, message: String? = null): TqValidation = test(field, "invalidEmail", message) {
        if (it !is String) {
            return@test false
        }
        return@test emailPattern.matcher(it.trim().toLowerCase()).matches()
    }

    fun length(field: String? = null, message: String? = null, min: Int? = null, max: Int? = null): TqValidation =
        test(field, "invalidLength", message) {
            if (it !is String) {
                return@test false
            }
            if (min != null && it.length < min) {
                return@test false
            }
            if (max != null && it.length > max) {
                return@test false
            }
            true
        }

    fun number(field: String? = null, message: String? = null): TqValidation = test(field, "invalidNumber", message) {
        return@test (it == null || it is Number)
    }

    fun string(field: String? = null, message: String? = null): TqValidation = test(field, "invalidString", message) {
        return@test (it == null || it is String)
    }

    fun arrayOf(field: String? = null, message: String? = null, check: TqValidation.() -> Unit): TqValidation =
        test(field, "invalidArray", message) {
            if (it !is Iterable<*>) {
                return@test false
            }

            it.forEachIndexed { i, element ->
                var name = fieldPath(field) ?: ""
                if (name.isNotEmpty()) {
                    name += '.'
                }

                val validation = TqValidation(element, name + i)
                check(validation)
                rejections.addAll(validation.rejections)
            }
            true
        }

    fun stop(data: Any? = null): Nothing = throw ValidationException(rejections, data)

    fun stopOnRejections(data: Any? = null): TqValidation {
        if (rejections.size > 0) {
            throw ValidationException(rejections, data)
        }
        return this
    }

    inline fun <reified E : Any> check(
        code: String,
        field: String,
        message: String? = null,
        check: (E?) -> Boolean
    ): TqValidation {
        if (src == null) {
            rejections.add(Rejection(code, fieldPath(field), message))
            return this
        }

        val test = getValue(src, field) as E?
        val validationCheckResult = check(test)
        tested++
        if (validationCheckResult) {
            passed++
        } else {
            rejections.add(Rejection(code, fieldPath(field), message))
        }

        return this
    }

    inline fun <reified T : Any?> with(newSrc: T = src as T, action: TqTypedValidation<T>.() -> Unit): TqValidation {
        val typedValidation = TqTypedValidation(newSrc)
        action(typedValidation)
        merge(typedValidation)
        return this
    }

    protected open fun test(field: String?, code: String, message: String?, check: (Any?) -> Boolean): TqValidation {
        if (src == null) {
            rejections.add(Rejection(code, fieldPath(field), message))
            return this
        }

        val test = getValue(src, field)
        val validationCheckResult = check(test)
        tested++
        if (validationCheckResult) {
            passed++
        } else {
            rejections.add(Rejection(code, fieldPath(field), message))
        }

        return this
    }

    fun fieldPath(field: String?): String? {
        if (path.isNotEmpty()) {
            if (field == null) {
                return path
            }
            return "$path.$field"
        }
        return field
    }

    fun getValue(src: Any, field: String?): Any? {
        if (field == null) {
            return src
        }
        var value = src
        field.split('.').forEach { key ->
            if (value is Map<*, *>) {
                value.javaClass.getMethod("get", Any::class.java).let {
                    it.isAccessible = true
                    value = it.invoke(value, key) ?: return null
                }
            } else {
                try {
                    value.javaClass.getMethod("get" + key.capitalize()).let {
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

    class TqTypedValidation<T>(typedSrc: T) : TqValidation(typedSrc) {
        fun check(code: String, message: String? = null, check: (T) -> Boolean): TqTypedValidation<T> {
            if (src == null) {
                rejections.add(Rejection(code, fieldPath(null), message))
                return this
            }

            val validationCheckResult = check(src as T)
            tested++
            if (validationCheckResult) {
                passed++
            } else {
                rejections.add(Rejection(code, fieldPath(null), message))
            }

            return this
        }
    }
}

