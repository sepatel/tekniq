package io.tekniq.config

open class TqMapConfig(values: Map<String, Any>) : TqConfig() {
    private var values: Map<String, Any> = values

    init {
        reload(values)
    }

    override fun reload() = reload(values)

    override fun reload(newConfigs: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val present = newConfigs.filterValues { it != null } as Map<String, Any>
        values = present
        super.reload(present)
    }

    fun reloadWithMap(newConfigs: Map<String, Any?>) = reload(newConfigs)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(key: String, type: Class<T>?): T? = values[key] as T?
}
