package io.tekniq.config

open class TqChainConfig(vararg private val confs: TqConfig) : TqConfig() {
    private val refConfigurations: MutableList<TqConfig>

    init {
        refConfigurations = mutableListOf<TqConfig>()
    }

    fun add(config: TqConfig) = refConfigurations.add(config)
    override fun reload() = confs.forEach { it.reload() }

    override fun <T> getValue(key: String, type: Class<T>?): T? {
        confs.forEach {
            val v = it.getValue(key, type)
            if (v != null) {
                return v
            }
        }
        return null
    }
}
