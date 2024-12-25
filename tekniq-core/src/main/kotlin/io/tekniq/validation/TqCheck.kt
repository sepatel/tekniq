package io.tekniq.validation

import java.net.URL
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KProperty

open class TqCheck(
    val src: Any? = null,
    val at: String? = null,
    val translator: TqCheckTranslator = DefaultCheckTranslator,
    private val path: String = ""
) {
    companion object {
        private val emailPattern =
            "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,10}\\b".toRegex(
                RegexOption.IGNORE_CASE
            )
        private val uuidPattern = "[a-f0-9]{8}-?([a-f0-9]{4}-?){3}[a-f0-9]{12}".toRegex(RegexOption.IGNORE_CASE)
        private val oidPattern = "[a-f0-9]{24}".toRegex(RegexOption.IGNORE_CASE)

        fun getValue(src: Any, field: String?): Any? {
            if (field == null) {
                return src
            }
            var value = src
            field.split('.').forEach { key ->
                if (value is Map<*, *>) {
                    value = value[key] ?: return null
                } else {
                    try {
                        val capitalized = key.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                            else it.toString()
                        }
                        value.javaClass.getMethod("get$capitalized").let {
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

    private var passed = 0
    private val _reasons = mutableListOf<DeniedReason>()
    val reasons: Collection<DeniedReason>
        get() = _reasons

    // Access the value directly
    fun get(field: KProperty<*>) = get(field.name)
    fun get(field: String? = null) = src?.let { getValue(it, field) }

    // Logical Operators
    fun and(message: String? = null, check: TqCheck.() -> Unit): TqCheck {
        val validation = TqCheck(src, at = at, path = path, translator = translator)
        check(validation)

        if (validation.reasons.isNotEmpty()) {
            _reasons += DefaultDeniedReason(
                "\$and [${validation.reasons.joinToString { it.code }}]",
                message ?: validation.reasons.joinToString { it.message })
        }
        return this
    }

    fun or(message: String? = null, check: TqCheck.() -> Unit): TqCheck {
        val validation = TqCheck(src, at = at, path = path, translator = translator)
        check(validation)

        if (validation.passed == 0) {
            _reasons += DefaultDeniedReason("\$or [${validation.reasons.joinToString { it.code }}]",
                message ?: validation.reasons.joinToString { it.message })
        }
        return this
    }

    private fun isDefined(field: String?): Boolean {
        if (src == null) return false

        var value = src
        field?.split('.')?.forEach {
            when (val v = value) {
                null -> return false
                is Map<*, *> -> {
                    if (!v.containsKey(it)) return false
                    value = v[it]
                }

                else -> try {
                    val capitalized = it.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault())
                        else char.toString()
                    }
                    v.javaClass.getMethod("get$capitalized").let { method ->
                        method.isAccessible = true
                        value = method.invoke(v)
                    }
                } catch (e: NoSuchMethodException) {
                    return false
                }
            }
        }

        return true
    }

    fun ifDefined(field: KProperty<*>, action: () -> Unit) = ifDefined(field.name, action)
    fun ifDefined(field: String? = null, action: () -> Unit): TqCheck {
        if (!isDefined(field)) return this
        action.invoke()
        return this
    }

    fun ifNotDefined(field: KProperty<*>, action: () -> Unit) = ifNotDefined(field.name, action)
    fun ifNotDefined(field: String? = null, action: () -> Unit): TqCheck {
        if (isDefined(field)) return this
        action.invoke()
        return this
    }

    fun listOf(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false, check: TqCheck.() -> Unit) =
        listOf(field.name, message, ifDefined, check)

    fun listOf(
        field: String? = null,
        message: String? = null,
        ifDefined: Boolean = false,
        check: TqCheck.() -> Unit
    ): TqCheck =
        test("Invalid", field, message, ifDefined) {
            if (it !is Iterable<*>) {
                return@test false
            }

            it.forEachIndexed { i, element ->
                var name = fieldPath(field) ?: ""
                if (name.isNotEmpty()) name += '.'

                val validation = TqCheck(element, at = at, path = name + i, translator = translator)
                check(validation)
                _reasons += validation.reasons
            }
            true
        }

    fun merge(vararg checks: TqCheck): TqCheck {
        checks.forEach {
            _reasons += it.reasons
            passed += it.passed
        }
        return this
    }

    // Validation Checks
    fun required(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false) =
        required(field.name, message, ifDefined)

    fun required(field: String, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("Required", field, message, ifDefined) {
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

    fun date(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false): TqCheck =
        date(field.name, message, ifDefined)

    fun date(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("InvalidDate", field, message, ifDefined) {
            return@test (it == null || it is Date || it is Temporal)
        }

    fun email(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false): TqCheck =
        email(field.name, message, ifDefined)

    fun email(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("InvalidEmail", field, message, ifDefined) {
            if (it !is String) return@test false
            return@test emailPattern.matches(it.trim())
        }

    fun length(
        field: KProperty<*>,
        message: String? = null,
        min: Int? = null,
        max: Int? = null,
        ifDefined: Boolean = false
    ): TqCheck = length(field.name, message, min, max, ifDefined)

    fun length(
        field: String? = null,
        message: String? = null,
        min: Int? = null,
        max: Int? = null,
        ifDefined: Boolean = false
    ): TqCheck =
        test("InvalidLength", field, message, ifDefined) {
            if (it !is String) return@test false
            if (min != null && it.length < min) return@test false
            if (max != null && it.length > max) return@test false
            true
        }

    fun notBlank(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false) =
        notBlank(field.name, message, ifDefined)

    fun notBlank(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("Blank", field, message, ifDefined) {
            return@test (it == null || it.toString().trim().isEmpty())
        }

    fun number(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false) =
        number(field.name, message, ifDefined)

    fun number(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("InvalidNumber", field, message, ifDefined) {
            return@test (it == null || it is Number)
        }

    fun string(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false) =
        string(field.name, message, ifDefined)

    fun string(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("InvalidString", field, message, ifDefined) {
            return@test (it == null || it is String)
        }

    fun oid(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false): TqCheck =
        oid(field.name, message, ifDefined)

    fun oid(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("InvalidOID", field, message, ifDefined) {
            if (it !is String) return@test false
            return@test oidPattern.matches(it.trim())
        }

    fun url(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false): TqCheck =
        url(field.name, message, ifDefined)

    fun url(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("InvalidURL", field, message, ifDefined) {
            if (it is URL) return@test true
            if (it !is String) return@test false
            try {
                URL(it)
                return@test true
            } catch (e: java.net.MalformedURLException) {
                return@test false
            }
        }

    fun uuid(field: KProperty<*>, message: String? = null, ifDefined: Boolean = false): TqCheck =
        uuid(field.name, message, ifDefined)

    fun uuid(field: String? = null, message: String? = null, ifDefined: Boolean = false): TqCheck =
        test("InvalidUUID", field, message, ifDefined) {
            if (it is UUID) return@test true
            if (it !is String) return@test false
            return@test uuidPattern.matches(it.trim())
        }

    fun deny(code: String, field: String? = null, message: String? = null) {
        _reasons += translator.translate(code, field, at, message, src)
    }

    // Flow Control
    fun denyImmediately(code: String, field: String? = null, message: String? = null): Nothing {
        deny(code, field, message)
        stop()
    }

    fun stop(): Nothing = throw CheckException(_reasons)

    fun stopOnDenials(): TqCheck {
        if (_reasons.isNotEmpty()) throw CheckException(_reasons)
        return this
    }

    protected fun test(
        code: String,
        field: KProperty<*>,
        message: String? = null,
        ifDefined: Boolean = false,
        check: (Any?) -> Boolean
    ) = test(code, field.name, message, ifDefined, check)

    protected open fun test(
        code: String,
        field: String? = null,
        message: String? = null,
        ifDefined: Boolean = false,
        check: (Any?) -> Boolean
    ): TqCheck {
        if (src == null) {
            deny(code, fieldPath(field), message)
            return this
        }

        val test = getValue(src, field)
        val checkResult =
            if (ifDefined) if (isDefined(field)) check(test) else true
            else check(test)
        if (!checkResult) deny(code, fieldPath(field), message)
        else passed++

        return this
    }

    protected fun fieldPath(field: String?): String? {
        if (path.isNotEmpty()) {
            if (field == null) {
                return path
            }
            return "$path.$field"
        }
        return field
    }
}
