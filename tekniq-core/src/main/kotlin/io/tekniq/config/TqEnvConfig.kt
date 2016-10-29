package io.tekniq.config

open class TqEnvConfig : TqConfig() {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(key: String, type: Class<T>?): T? = System.getenv(key) as T
}

