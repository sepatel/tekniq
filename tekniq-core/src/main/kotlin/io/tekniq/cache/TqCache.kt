package io.tekniq.cache

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

open class TqCache<K, V>(val expireAfterAccess: Long? = null,
                         val expireAfterWrite: Long? = null,
                         val maximumSize: Int? = null,
                         val recordStats: Boolean = false,
                         private val loader: (key: K) -> V) : Map<K, V> {
    val stats: TqCacheStats // read-only version of the actual data
        get() = TqCacheStats(hits.get(), misses.get())
    private val hits = AtomicInteger()
    private val misses = AtomicInteger()

    private val map = ConcurrentHashMap<K, TqCacheElement<V>>() // TODO: Need a concurrent linked hash map implementation.
    private val creationOrder = ConcurrentLinkedQueue<K>() // TODO: This is a hack and not truly thread-safe for creation order logic
    private val lock = ReentrantReadWriteLock()

    override val entries: Set<Map.Entry<K, V>>
        get() = map.entries.map {
            AbstractMap.SimpleEntry(it.key, it.value.value)
        }.toSet()
    override val keys: Set<K>
        get() = map.keys
    override val values: Collection<V>
        get() = map.entries.map { it.value.value }
    override val size: Int
        get() = map.size

    override fun containsKey(key: K): Boolean = map.containsKey(key) // TODO: Remove entry if expired

    override fun containsValue(value: V): Boolean = map.containsValue(value ?: false) // TODO: Remove entry if expired

    override fun get(key: K): V {
        val now = System.currentTimeMillis()
        var elem: TqCacheElement<V>
        if (map.containsKey(key)) {
            elem = map[key]!!
            if ((expireAfterAccess != null && now > elem.accessed + expireAfterAccess)
                    || (expireAfterWrite != null && now > elem.created + expireAfterWrite)) {
                elem = TqCacheElement(loader.invoke(key), now, now)
                map.put(key, elem)
                if (recordStats) {
                    misses.andIncrement
                }
            } else if (recordStats) {
                hits.andIncrement
            }
        } else {
            while (maximumSize != null && map.size >= maximumSize) {
                lock.write { map.remove(creationOrder.poll()) }
            }
            elem = TqCacheElement(loader.invoke(key), now, now)
            map.put(key, elem)
            lock.write { creationOrder.add(key) }
            if (recordStats) {
                misses.andIncrement
            }
        }

        elem.accessed = now
        return elem.value
    }

    override fun isEmpty(): Boolean = map.isEmpty()

    fun invalidate(key: K) {
        lock.write {
            map.remove(key)
            creationOrder.remove(key)
        }
    }

    fun invalidateAll() {
        lock.write {
            map.clear()
            creationOrder.clear()
        }
    }

    fun seed(key: K, value: V) {
        lock.write {
            map.put(key, TqCacheElement(value))
            creationOrder.remove(key)
            creationOrder.add(key)
        }
    }

    fun seed(entries: Map<K, V>, replaceConflicts: Boolean = true) {
        entries.entries.forEach {
            if (map.containsKey(it.key)) {
                if (replaceConflicts) {
                    map.put(it.key, TqCacheElement(it.value))
                }
            } else {
                lock.write {
                    map.put(it.key, TqCacheElement(it.value))
                    creationOrder.add(it.key)
                }
            }
        }
    }

    fun clean() {
        val now = System.currentTimeMillis()
        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val elem = iterator.next()
            if ((expireAfterAccess != null && now > elem.value.accessed + expireAfterAccess)
                    || (expireAfterWrite != null && now > elem.value.created + expireAfterWrite)) {
                lock.write {
                    creationOrder.remove(elem.key)
                    iterator.remove()
                }
            }
        }
    }
}

data class TqCacheStats(var hits: Int = 0, var misses: Int = 0)
private data class TqCacheElement<out V>(val value: V, var accessed: Long = System.currentTimeMillis(), val created: Long = System.currentTimeMillis())

