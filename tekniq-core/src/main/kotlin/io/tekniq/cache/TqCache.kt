package io.tekniq.cache

@Deprecated("Please use TqCaffeine from tekniq-cache library instead")
open class TqCache<K, V>(
    val expireAfterAccess: Long? = null,
    val expireAfterWrite: Long? = null,
    val maximumSize: Long? = null,
    val recordStats: Boolean = false,
    private val loader: (key: K) -> V?
) : TqCacheMap<K, V> {
    init {
        TODO("Switch from TqCache to TqCaffeine located in the tekniq-cache module")
    }

    override val stats: TqCacheStats // read-only version of the actual data
        get() = TODO("Not yet implemented")

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TODO("Not yet implemented")
    override val keys: MutableSet<K>
        get() = TODO("Not yet implemented")
    override val values: MutableCollection<V>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun containsKey(key: K): Boolean = TODO("Not yet implemented")
    override fun containsValue(value: V): Boolean = TODO("Not yet implemented")
    override fun get(key: K): V? = TODO("Not yet implemented")
    override fun isEmpty(): Boolean = TODO("Not yet implemented")
    override fun remove(key: K): V? = TODO("Not yet implemented")
    override fun clear() = TODO("Not yet implemented")
    override fun put(key: K, value: V): V? = TODO("Not yet implemented")
    override fun putAll(from: Map<out K, V>) = TODO("Not yet implemented")
    override fun cleanUp() {
        TODO("Not yet implemented")
    }
}
