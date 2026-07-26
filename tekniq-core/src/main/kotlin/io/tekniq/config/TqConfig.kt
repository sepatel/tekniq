package io.tekniq.config

abstract class TqConfig {
    private val configs: MutableMap<String, Any?> = mutableMapOf()

    /** A snapshot, so it stays safe to iterate while another thread reloads. */
    open val keys: Set<String>
        get() = synchronized(configs) { configs.keys.toSet() }

    open fun contains(key: String): Boolean = get<Any>(key) != null

    inline fun <reified T : Any> get(key: String): T? = get(key, T::class.java)

    // Reads memoise into a map shared by every caller, so all of them have to hold the monitor.
    // Leaving this unguarded let concurrent reads corrupt the map's own table and return values
    // belonging to other keys. synchronized is reentrant, so a getValue that reads back in is fine.
    open fun <T> get(key: String, type: Class<T>?): T? = synchronized(configs) {
        if (!configs.containsKey(key)) {
            val value = getValue(key, type) ?: return null
            configs[key] = value
        }

        @Suppress("UNCHECKED_CAST")
        configs[key] as T // Allow the casting exception. Cannot make an array to an Int for example.
    }

    /** Drops memoised reads. Decorators must call this when the values underneath them change. */
    protected fun clearCache() = synchronized(configs) { configs.clear() }

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

    open fun reload() {
        // Override in subclasses to implement custom reload logic
    }

    /** Replaces the contents wholesale: keys absent from [newConfigs] are removed, not merged. */
    open fun reload(newConfigs: Map<String, Any?>) = synchronized(configs) {
        configs.keys.retainAll(newConfigs.keys)
        configs.putAll(newConfigs)
    }
}
