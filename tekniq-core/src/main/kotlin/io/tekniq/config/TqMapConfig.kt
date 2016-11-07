package io.tekniq.config

import java.util.*

open class TqMapConfig(private val values: Map<String, Any>) : TqConfig() {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(key: String, type: Class<T>?): T? = values[key] as T
}

open class TqPropertiesConfig(private val propertiesFile: String) : TqMapConfig(Properties().apply { load(javaClass.getResourceAsStream(propertiesFile)) } as Map<String, Any>)