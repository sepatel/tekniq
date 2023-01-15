package io.tekniq.config

abstract class TqConfig {
    val keys: Set<String>
        get() = configs.keys

    private val configs: MutableMap<String, Any?> = mutableMapOf()

    open fun contains(key: String): Boolean {
        if (!configs.containsKey(key)) {
            getValue<Any?>(key) // load it into the configs in case this is the first check
        }
        return configs.containsKey(key)
    }

    inline fun <reified T : Any> get(key: String): T? = get(key, T::class.java)
    open fun <T> get(key: String, type: Class<T>?): T? {
        if (!configs.containsKey(key)) { // load config into the cache if missing
            val value = getValue(key, type) ?: return null
            configs[key] = value
        }

        @Suppress("UNCHECKED_CAST")
        return configs[key] as T // Allow the casting exception. Cannot make an array to an Int for example.
    }

    open fun getDouble(key: String): Double? {
        val any = get<Any>(key) ?: return null
        when (any) {
            is Number -> return any.toDouble()
            is String -> return any.toDouble()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Double")
    }

    open fun getFloat(key: String): Float? {
        val any = get<Any>(key) ?: return null
        when (any) {
            is Number -> return any.toFloat()
            is String -> return any.toFloat()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Float")
    }

    open fun getInt(key: String): Int? {
        val any = get<Any>(key) ?: return null
        when (any) {
            is Number -> return any.toInt()
            is String -> return any.toDouble().toInt()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Int")
    }

    open fun getLong(key: String): Long? {
        val any = get<Any>(key) ?: return null
        when (any) {
            is Number -> return any.toLong()
            is String -> return any.toDouble().toLong()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Long")
    }

    open fun getShort(key: String): Short? {
        val any = get<Any>(key) ?: return null
        when (any) {
            is Number -> return any.toShort()
            is String -> return any.toDouble().toInt().toShort()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Short")
    }

    abstract fun <T : Any?> getValue(key: String, type: Class<T>? = null): T?

    open fun reload() = Unit

    protected fun reload(newConfigs: Map<String, Any?>) {
        val existing = HashSet(configs.keys)
        newConfigs.entries.forEach {
            val oldValue = configs[it.key]
            if (!configs.containsKey(it.key) || oldValue != it.value) {
                configs[it.key] = it.value
            }
            existing.remove(it.key)
        }
        existing.forEach { configs.remove(it) }
    }
}
