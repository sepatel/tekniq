package io.tekniq.config

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

internal class TqConfigBinder<T : Any>(
    private val kClass: KClass<T>,
    private val config: TqConfig
) {
    private val ctor = kClass.primaryConstructor
        ?: throw TqConfigBindException(
            message = "Cannot bind: class has no primary constructor",
            expectedType = kClass.simpleName
        )

    private val annotationPrefix: String? = kClass.findAnnotation<TqConfigPrefix>()?.value

    fun bind(explicitPrefix: String?): T {
        val prefix = annotationPrefix ?: explicitPrefix ?: ""
        val args = mutableMapOf<KParameter, Any?>()
        ctor.parameters.forEach { param ->
            val resolved = resolveParam(param, prefix)
            when {
                resolved != null -> args[param] = resolved
                param.type.isMarkedNullable -> args[param] = null
                param.isOptional -> { /* omit, callBy will use default */ }
                else -> throw TqConfigBindException(
                    message = "Required value missing",
                    path = keyFor(param, prefix)
                )
            }
        }
        return ctor.callBy(args)
    }

    private fun resolveParam(param: KParameter, parentPrefix: String): Any? {
        val name = param.name
            ?: throw TqConfigBindException(
                message = "Parameter has no name; cannot resolve",
                expectedType = kClass.simpleName
            )
        val key = if (parentPrefix.isEmpty()) name else "$parentPrefix.$name"
        val classifier = (param.type.classifier as? KClass<*>) ?: throw TqConfigBindException(
            message = "Unsupported parameter type",
            path = key,
            expectedType = param.type.toString()
        )
        return when {
            isCollection(classifier) -> resolveList(key, param)
            isMap(classifier) -> resolveMap(key, param)
            isScalarLeaf(classifier) -> resolveScalar(key, classifier)
            isBindableNestedClass(classifier) -> resolveNested(key, classifier)
            else -> throw TqConfigBindException(
                message = "Unsupported parameter type",
                path = key,
                expectedType = classifier.simpleName
            )
        }
    }

    private fun isScalarLeaf(classifier: KClass<*>): Boolean {
        return classifier == String::class ||
            classifier == Char::class ||
            classifier == Boolean::class ||
            classifier.java.isPrimitive ||
            (classifier.java.isEnum) ||
            isBoxedPrimitive(classifier)
    }

    private fun isBoxedPrimitive(classifier: KClass<*>): Boolean {
        return classifier == java.lang.Integer::class ||
            classifier == java.lang.Long::class ||
            classifier == java.lang.Short::class ||
            classifier == java.lang.Byte::class ||
            classifier == java.lang.Double::class ||
            classifier == java.lang.Float::class ||
            classifier == java.lang.Boolean::class ||
            classifier == java.lang.Character::class ||
            classifier == kotlin.Number::class
    }

    private fun isCollection(classifier: KClass<*>): Boolean {
        return classifier == List::class ||
            classifier == Set::class ||
            classifier == Collection::class ||
            classifier == Iterable::class
    }

    private fun isMap(classifier: KClass<*>): Boolean {
        return classifier == Map::class
    }

    private fun isBindableNestedClass(classifier: KClass<*>): Boolean {
        return classifier != Any::class &&
            classifier != Unit::class &&
            !classifier.java.isArray &&
            !classifier.isSealed &&
            classifier.primaryConstructor != null
    }

    private fun resolveScalar(key: String, classifier: KClass<*>): Any? {
        val raw = lookup(key) ?: return null
        return try {
            TqConfigTypeCoercion.coerce(raw, classifier)
        } catch (e: TqConfigBindException) {
            throw TqConfigBindException(
                message = e.message ?: "Type coercion failed",
                path = key,
                expectedType = e.expectedType ?: classifier.simpleName,
                actualValue = e.actualValue ?: raw,
                cause = e
            )
        }
    }

    private fun resolveNested(key: String, classifier: KClass<*>): Any? {
        val raw = lookup(key)
        if (raw is Map<*, *>) {
            val stringKeyed: Map<String, Any?> = raw.entries
                .mapNotNull { (k, v) -> if (k == null) null else k.toString() to v }
                .toMap()
            val elementConfig = TqConfigFromNullableMap(stringKeyed)
            return TqConfigBinder(classifier, elementConfig).bind(null)
        }
        val innerConfig = TqConfigNestedView(config, key)
        return TqConfigBinder(classifier, innerConfig).bind(null)
    }

    private fun resolveList(key: String, param: KParameter): Any? {
        val elementClass = param.type.arguments.firstOrNull()?.type?.let { it.classifier as? KClass<*> }
            ?: throw TqConfigBindException(
                message = "Cannot determine List element type",
                path = key
            )
        val raw = lookup(key) ?: return null
        return when (raw) {
            is List<*> -> bindList(raw, elementClass, key)
            is Iterable<*> -> bindList(raw.toList(), elementClass, key)
            is String -> TqConfigTypeCoercion.coerceCommaSeparated(raw, elementClass)
            else -> throw TqConfigBindException(
                message = "Cannot coerce to List",
                path = key,
                expectedType = "List<${elementClass.simpleName}>",
                actualValue = raw
            )
        }
    }

    private fun bindList(elements: List<*>, elementClass: KClass<*>, key: String): List<Any?> {
        if (isScalarLeaf(elementClass)) {
            return TqConfigTypeCoercion.coerceList(elements, elementClass)
        }
        if (isBindableNestedClass(elementClass)) {
            return elements.mapIndexed { i, element ->
                val elementMap = (element as? Map<*, *>)
                    ?: throw TqConfigBindException(
                        message = "List element is not a Map",
                        path = "$key[$i]",
                        expectedType = "Map (for ${elementClass.simpleName})",
                        actualValue = element
                    )
                val stringKeyed: Map<String, Any?> = elementMap.entries
                    .mapNotNull { (k, v) -> if (k == null) null else k.toString() to v }
                    .toMap()
                val elementConfig = TqConfigFromNullableMap(stringKeyed)
                TqConfigBinder(elementClass, elementConfig).bind(null)
            }
        }
        return TqConfigTypeCoercion.coerceList(elements, elementClass)
    }

    private fun resolveMap(key: String, param: KParameter): Any? {
        val valueClass = param.type.arguments.getOrNull(1)?.type?.let { it.classifier as? KClass<*> }
            ?: throw TqConfigBindException(
                message = "Cannot determine Map value type",
                path = key
            )
        val resolved = lookupMap(key, valueClass) ?: return null
        return resolved
    }

    private fun lookupMap(prefix: String, valueClass: KClass<*>): Map<String, Any?>? {
        val parts = prefix.split(".")
        val walked = walkNested(parts)
        if (walked is Map<*, *>) {
            return walked.entries.associate { (k, v) ->
                k.toString() to coerceMapValue(v, valueClass, "$prefix.${k}")
            }
        }
        if (config.contains(prefix)) {
            @Suppress("UNCHECKED_CAST")
            val v = config.get<Any>(prefix)
            if (v is Map<*, *>) {
                return v.entries.associate { (k, v2) ->
                    k.toString() to coerceMapValue(v2, valueClass, "$prefix.${k}")
                }
            }
        }
        val dotPrefix = "$prefix."
        val matches = config.keys.filter { it.startsWith(dotPrefix) }
        if (matches.isEmpty()) return null
        return matches.associate { fullKey ->
            val subKey = fullKey.removePrefix(dotPrefix)
            val raw = if (config.contains(fullKey)) config.get<Any>(fullKey) else null
            subKey to coerceMapValue(raw, valueClass, fullKey)
        }
    }

    private fun coerceMapValue(value: Any?, valueClass: KClass<*>, path: String): Any? {
        if (value == null) return null
        return try {
            TqConfigTypeCoercion.coerce(value, valueClass)
        } catch (e: TqConfigBindException) {
            throw TqConfigBindException(
                message = e.message ?: "Type coercion failed",
                path = path,
                expectedType = valueClass.simpleName,
                actualValue = value,
                cause = e
            )
        }
    }

    private fun lookup(dottedKey: String): Any? {
        if (config.contains(dottedKey)) {
            @Suppress("UNCHECKED_CAST")
            return config.get<Any>(dottedKey)
        }
        return walkNested(dottedKey.split("."))
    }

    private fun walkNested(parts: List<String>): Any? {
        if (parts.isEmpty()) return null
        val head = parts.first()
        val rest = parts.drop(1)
        if (!config.contains(head)) return null
        @Suppress("UNCHECKED_CAST")
        var current: Any? = config.get<Any>(head)
        for (segment in rest) {
            if (current !is Map<*, *>) return null
            current = current[segment]
        }
        return current
    }

    private fun keyFor(param: KParameter, parentPrefix: String): String? {
        val name = param.name ?: return null
        return if (parentPrefix.isEmpty()) name else "$parentPrefix.$name"
    }
}

internal class TqConfigNestedView(
    private val delegate: TqConfig,
    private val prefix: String
) : TqConfig() {
    override fun <T : Any?> getValue(key: String, type: Class<T>?): T? {
        val combined = if (prefix.isEmpty()) key else "$prefix.$key"
        @Suppress("UNCHECKED_CAST")
        return delegate.get(combined, type) as T?
    }

    override fun contains(key: String): Boolean {
        val combined = if (prefix.isEmpty()) key else "$prefix.$key"
        return delegate.contains(combined)
    }

    override fun reload() {
        delegate.reload()
    }

    override fun reload(newConfigs: Map<String, Any?>) {
        delegate.reload(newConfigs)
    }
}

internal class TqConfigFromNullableMap(
    private val values: Map<String, Any?>
) : TqConfig() {
    override fun <T : Any?> getValue(key: String, type: Class<T>?): T? {
        @Suppress("UNCHECKED_CAST")
        return values[key] as T?
    }

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun reload() {}

    override fun reload(newConfigs: Map<String, Any?>) {}

    val keysView: Set<String> get() = values.keys
}
