package io.tekniq.cache

import io.tekniq.cache.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder
import java.util.AbstractMap.SimpleEntry
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.MutableMap.MutableEntry

@Deprecated("Please use TqCaffeine from tekniq-cache library instead")
open class TqCache<K, V>(val expireAfterAccess: Long? = null,
                         val expireAfterWrite: Long? = null,
                         val maximumSize: Long? = null,
                         val recordStats: Boolean = false,
                         private val loader: (key: K) -> V?) : TqCacheMap<K, V> {
    override val stats: TqCacheStats // read-only version of the actual data
        get() = TqCacheStats(hits.get(), misses.get())
    private val hits = AtomicLong()
    private val misses = AtomicLong()

    private val map = Builder<K, TqCacheElement<V>>().maximumWeightedCapacity(maximumSize ?: Long.MAX_VALUE).build()

    override val entries: MutableSet<MutableEntry<K, V>>
        get() = map.entries.map {
            SimpleEntry(it.key, it.value.value)
        }.toMutableSet()
    override val keys: MutableSet<K>
        get() = map.keys
    override val values: MutableCollection<V>
        get() = map.entries.map { it.value.value }.toMutableList()
    override val size: Int
        get() = map.size

    override fun containsKey(key: K): Boolean {
        val result = map.containsKey(key)
        if (result) {
            val elem = map[key]
            val now = System.currentTimeMillis()
            if (elem == null || (expireAfterAccess != null && now > elem.accessed + expireAfterAccess)
                    || (expireAfterWrite != null && now > elem.created + expireAfterWrite)) {
                map.remove(key)
                return false
            }
        }
        return result
    }

    override fun containsValue(value: V): Boolean = map.containsValue(value ?: false) // TODO: Remove entry if expired

    override fun get(key: K): V? {
        val now = System.currentTimeMillis()
        var elem: TqCacheElement<V?>
        if (map.containsKey(key)) {
            elem = map[key]!!
            if ((expireAfterAccess != null && now > elem.accessed + expireAfterAccess)
                    || (expireAfterWrite != null && now > elem.created + expireAfterWrite)) {
                if (recordStats) {
                    misses.andIncrement
                }
                val value = loader.invoke(key)
                if (value != null) {
                    elem = TqCacheElement(value, now, now)
                    map.put(key, elem)
                } else {
                    map.remove(key)
                    return null
                }
            } else if (recordStats) {
                hits.andIncrement
            }
        } else {
            if (recordStats) {
                misses.andIncrement
            }
            val value = loader.invoke(key)
            if (value != null) {
                elem = TqCacheElement(value, now, now)
                map.put(key, elem)
            } else {
                map.remove(key)
                return null
            }
        }

        elem.accessed = now
        return elem.value
    }

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun remove(key: K): V? = map.remove(key)?.value

    override fun clear() = map.clear()

    override fun put(key: K, value: V): V? {
        val elem = map.put(key, TqCacheElement(value)) ?: return null
        val now = System.currentTimeMillis()
        if ((expireAfterAccess != null && now > elem.accessed + expireAfterAccess)
                || (expireAfterWrite != null && now > elem.created + expireAfterWrite)) {
            return null
        }
        return value
    }

    override fun putAll(from: Map<out K, V>) = from.entries.forEach {
        if (it.value != null) {
            map.put(it.key, TqCacheElement(it.value))
        }
    }

    override fun cleanUp() {
        val now = System.currentTimeMillis()
        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val elem = iterator.next()
            if ((expireAfterAccess != null && now > elem.value.accessed + expireAfterAccess)
                    || (expireAfterWrite != null && now > elem.value.created + expireAfterWrite)) {
                iterator.remove()
            }
        }
    }
}

private data class TqCacheElement<out V>(val value: V, var accessed: Long = System.currentTimeMillis(), val created: Long = System.currentTimeMillis())

