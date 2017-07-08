package io.tekniq.cache

interface TqCacheMap<K, V> : MutableMap<K, V> {
    val stats: TqCacheStats
    fun cleanUp()
}