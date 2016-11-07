package io.tekniq.config

class TqChainConfig(vararg private val config: TqConfig) : TqConfig() {
    override fun <T> getValue(key: String, type: Class<T>?): T? {
        config.forEach {
            val v = it.getValue(key, type)
            if (v != null) {
                return v
            }
        }
        return null
    }
}
