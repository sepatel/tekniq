package io.tekniq.config

interface TqConfigObserver {
    fun unregister(): Boolean
}

abstract class TqConfig {
    val keys: Set<String>
        get() = configs.keys

    private val configs: MutableMap<String, Any?> = mutableMapOf()
    private val observers = arrayListOf<DefaultTqConfigObserver>()

    open fun contains(key: String): Boolean {
        if (!configs.containsKey(key)) {
            getValue<Any?>(key) // load it into the configs in case this is the first check
        }
        return configs.containsKey(key)
    }

    inline fun <reified T : Any> get(key: String, defaultValue: T? = null): T? = get(key, defaultValue, T::class.java)
    open fun <T> get(key: String, defaultValue: T? = null, type: Class<T>?): T? {
        if (!configs.containsKey(key)) { // load config into the cache if missing
            val value = getValue(key, type) ?: return defaultValue
            configs[key] = value
        }

        @Suppress("UNCHECKED_CAST")
        return configs[key] as T // Allow the casting exception. Cannot make an array to an Int for example.
    }

    open fun getDouble(key: String, defaultValue: Double? = null): Double? {
        val any = get<Any>(key) ?: return defaultValue
        when (any) {
            is Number -> return any.toDouble()
            is String -> return any.toDouble()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Double")
    }

    open fun getFloat(key: String, defaultValue: Float? = null): Float? {
        val any = get<Any>(key) ?: return defaultValue
        when (any) {
            is Number -> return any.toFloat()
            is String -> return any.toFloat()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Float")
    }

    open fun getInt(key: String, defaultValue: Int? = null): Int? {
        val any = get<Any>(key) ?: return defaultValue
        when (any) {
            is Number -> return any.toInt()
            is String -> return any.toDouble().toInt()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Int")
    }

    open fun getLong(key: String, defaultValue: Long? = null): Long? {
        val any = get<Any>(key) ?: return defaultValue
        when (any) {
            is Number -> return any.toLong()
            is String -> return any.toDouble().toLong()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Long")
    }

    open fun getShort(key: String, defaultValue: Short? = null): Short? {
        val any = get<Any>(key) ?: return defaultValue
        when (any) {
            is Number -> return any.toShort()
            is String -> return any.toDouble().toInt().toShort()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Short")
    }

    fun onChange(key: String? = null, callback: TqConfigObserver.(key: String, value: Any?, oldValue: Any?) -> Unit) =
        observers.add(DefaultTqConfigObserver(key, observers, callback))

    abstract fun <T : Any?> getValue(key: String, type: Class<T>? = null): T?

    open fun reload() = Unit

    protected fun reload(newConfigs: Map<String, Any?>) {
        val existing = HashSet(configs.keys)
        newConfigs.entries.forEach {
            val oldValue = configs[it.key]
            if (!configs.containsKey(it.key) || oldValue != it.value) {
                configs[it.key] = it.value
                changed(it.key, oldValue, it.value)
            }
            existing.remove(it.key)
        }
        existing.forEach {
            val oldValue = configs.remove(it)
            if (oldValue != null) { // defined null or undefined null is still the same thing
                changed(it, null, oldValue)
            }
        }
    }

    private fun changed(key: String, oldValue: Any?, newValue: Any?) = observers.forEach {
        if (it.key == null || key == it.key) {
            it.callback.invoke(it, key, newValue, oldValue)
        }
    }
}

private class DefaultTqConfigObserver(
    val key: String?,
    val observers: ArrayList<DefaultTqConfigObserver>,
    val callback: TqConfigObserver.(String, Any?, Any?) -> Unit
) : TqConfigObserver {
    override fun unregister() = observers.remove(this)
}
