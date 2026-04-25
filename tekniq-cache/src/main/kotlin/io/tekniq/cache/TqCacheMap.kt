package io.tekniq.cache

interface TqCacheMap<K : Any, V : Any> : MutableMap<K, V> {
    val stats: TqCacheStats
    fun cleanUp()
}