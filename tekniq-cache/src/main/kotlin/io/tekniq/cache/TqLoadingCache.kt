package io.tekniq.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.collections.MutableMap.MutableEntry

open class TqLoadingCache<K : Any, V : Any>(
    val expireAfterAccess: Long? = null,
    val expireAfterWrite: Long? = null,
    val refreshAfterWrite: Long? = null,
    val maximumSize: Long? = null,
    val recordStats: Boolean = false,
    private val loader: (key: K) -> V?
) : TqCacheMap<K, V> {
    private val cache: Cache<K, V> = Caffeine
        .newBuilder()
        .also { builder ->
            expireAfterAccess?.also { builder.expireAfterAccess(expireAfterAccess, MILLISECONDS) }
            expireAfterWrite?.also { builder.expireAfterWrite(expireAfterWrite, MILLISECONDS) }
            maximumSize?.also { builder.maximumSize(maximumSize) }
            if (recordStats) builder.recordStats()
        }
        .build()
    private val map = cache.asMap()
    override val entries: MutableSet<MutableEntry<K, V>>
        get() = map.entries
    override val keys: MutableSet<K>
        get() = map.keys
    override val size: Int
        get() = cache.estimatedSize().toInt()
    override val stats: TqCacheStats
        get() = cache.stats().let {
            TqCacheStats(
                hitCount = it.hitCount(),
                missCount = it.missCount(),
                loadSuccessCount = it.loadSuccessCount(),
                loadFailureCount = it.loadFailureCount(),
                totalLoadTime = it.totalLoadTime(),
                evictionCount = it.evictionCount(),
                evictionWeight = it.evictionWeight(),
            )
        }
    override val values: MutableCollection<V>
        get() = map.values

    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun clear() = map.clear()
    override fun put(key: K, value: V): V? = map.put(key, value)
    override fun putAll(from: Map<out K, V>) = map.putAll(from)
    override fun remove(key: K): V? = map.remove(key)
    override fun get(key: K): V? {
        cache.getIfPresent(key)?.let { return it }
        val loaded = loader(key) ?: return null
        cache.put(key, loaded)
        return loaded
    }
    override fun cleanUp() = cache.cleanUp()
}

