package io.tekniq.config

open class TqChainConfig(private vararg val confs: TqConfig) : TqConfig() {
    private val refConfigurations: MutableList<TqConfig> = mutableListOf()

    fun add(config: TqConfig) = refConfigurations.add(config)
    override fun contains(key: String): Boolean {
        confs.forEach {
            if (it.contains(key)) {
                return true
            }
        }
        return false
    }

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
