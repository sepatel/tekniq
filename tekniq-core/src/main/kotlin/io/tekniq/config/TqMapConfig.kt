package io.tekniq.config

open class TqMapConfig(values: Map<String, Any>) : TqConfig() {
    private var values: Map<String, Any> = values
        private set

    init {
        reload(values)
    }

    override fun reload() = reload(values)

    override fun reload(newConfigs: Map<String, Any?>) {
        super.reload(newConfigs.filterValues { it != null }.mapValues { it.value as Any })
        values = newConfigs.filterValues { it != null }.mapValues { it.value as Any }
    }

    fun reloadWithMap(newConfigs: Map<String, Any?>) = reload(newConfigs)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(key: String, type: Class<T>?): T? = values[key] as T?
}
