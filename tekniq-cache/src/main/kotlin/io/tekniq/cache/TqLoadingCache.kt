package io.tekniq.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.collections.MutableMap.MutableEntry

typealias TqCaffeine<K, V> = TqLoadingCache<K, V>

open class TqLoadingCache<K, V>(
    val expireAfterAccess: Long? = null,
    val expireAfterWrite: Long? = null,
    val refreshAfterWrite: Long? = null,
    val maximumSize: Long? = null,
    val recordStats: Boolean = false,
    private val loader: (key: K) -> V?
) : TqCacheMap<K, V> {
    private val cacheLoader: LoadingCache<K, V> = Caffeine
        .newBuilder()
        .also { builder ->
            expireAfterAccess?.let { builder.expireAfterAccess(expireAfterAccess, MILLISECONDS) }
            expireAfterWrite?.let { builder.expireAfterWrite(expireAfterWrite, MILLISECONDS) }
            maximumSize?.let { builder.maximumSize(maximumSize) }
            refreshAfterWrite?.let { builder.refreshAfterWrite(refreshAfterWrite, MILLISECONDS) }
            if (recordStats) {
                builder.recordStats()
            }
        }
        .build { key -> loader(key) }
    private val map = cacheLoader.asMap()
    override val entries: MutableSet<MutableEntry<K, V>>
        get() = map.entries
    override val keys: MutableSet<K>
        get() = map.keys
    override val size: Int
        get() = cacheLoader.estimatedSize().toInt()
    override val stats: TqCacheStats
        get() = cacheLoader.stats().let { TqCacheStats(it.hitCount(), it.missCount()) }
    override val values: MutableCollection<V>
        get() = map.values

    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun clear() = map.clear()
    override fun put(key: K, value: V): V? = map.put(key, value)
    override fun putAll(from: Map<out K, V>) = map.putAll(from)
    override fun remove(key: K): V? = map.remove(key)
    override fun get(key: K): V? = cacheLoader.get(key)
    override fun cleanUp() = cacheLoader.cleanUp()
}

