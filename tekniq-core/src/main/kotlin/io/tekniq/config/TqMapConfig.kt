package io.tekniq.config

open class TqMapConfig(values: Map<String, Any>) : TqConfig() {
    init {
        configs.putAll(values)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(key: String, type: Class<T>?): T? = configs[key] as T

    override fun getDouble(key: String, defaultValue: Number?): Double? {
        if (!contains(key)) {
            return null
        }
        val any = get<Any>(key)
        if (any == null) {
            return null
        } else if (any is Number) {
            return super.getDouble(key, defaultValue)
        } else if (any is String) {
            return any.toDouble()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Double")
    }

    override fun getFloat(key: String, defaultValue: Number?): Float? {
        if (!contains(key)) {
            return null
        }
        val any = get<Any>(key)
        if (any == null) {
            return null
        } else if (any is Number) {
            return super.getFloat(key, defaultValue)
        } else if (any is String) {
            return any.toFloat()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Float")
    }

    override fun getLong(key: String, defaultValue: Number?): Long? {
        if (!contains(key)) {
            return null
        }
        val any = get<Any>(key)
        if (any == null) {
            return null
        } else if (any is Number) {
            return super.getLong(key, defaultValue)
        } else if (any is String) {
            return any.toLong()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Long")
    }

    override fun getShort(key: String, defaultValue: Number?): Short? {
        if (!contains(key)) {
            return null
        }
        val any = get<Any>(key)
        if (any == null) {
            return null
        } else if (any is Number) {
            return super.getShort(key, defaultValue)
        } else if (any is String) {
            return any.toShort()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Short")
    }

    override fun getInt(key: String, defaultValue: Number?): Int? {
        if (!contains(key)) {
            return null
        }
        val any = get<Any>(key)
        if (any == null) {
            return null
        } else if (any is Number) {
            return super.getInt(key, defaultValue)
        } else if (any is String) {
            return any.toInt()
        }
        throw IllegalStateException("Type ${any.javaClass.name} cannot be converted to Int")
    }
}
