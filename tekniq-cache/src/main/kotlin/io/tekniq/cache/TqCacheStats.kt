package io.tekniq.cache

data class TqCacheStats(
    val hitCount: Long,
    val missCount: Long,
    val loadSuccessCount: Long,
    val loadFailureCount: Long,
    val totalLoadTime: Long,
    val evictionCount: Long,
    val evictionWeight: Long,
)
