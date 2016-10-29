package io.tekniq.cache

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.concurrent.thread

class TqCacheTest {
    @Test fun simpleHit() {
        val cache = TqCache(recordStats = true, loader = String::toInt)
        validateStatistics(cache, 0, 0)

        1.rangeTo(10).forEach {
            val answer = cache["42"]
            assertEquals(42, answer)
            validateStatistics(cache, it - 1, 1)
        }
    }

    @Test fun zeroHitsOnThreadedUniques() {
        val cache = TqCache(recordStats = true, loader = String::toInt)
        validateStatistics(cache, 0, 0)

        val threads = mutableListOf<Thread>()
        1.rangeTo(20).forEach { i -> // spawn 20 threads
            threads.add(thread {
                val answer = cache[i.toString()]
                assertEquals(i, answer) // seed the cache
            })
        }

        threads.forEach(Thread::join)
        validateStatistics(cache, 0, 20)
    }

    @Test fun multiThreadedUniqueHits() {
        val cache = TqCache(recordStats = true, loader = String::toInt)
        validateStatistics(cache, 0, 0)

        val threads = mutableListOf<Thread>()
        1.rangeTo(20).forEach { i -> // spawn 20 threads
            threads.add(thread {
                1.rangeTo(100).forEach { x -> // each thread hits 100 times
                    val answer = cache[i.toString()]
                    assertEquals(i, answer) // seed the cache
                }
            })
        }

        threads.forEach(Thread::join)
        validateStatistics(cache, 20 * 100 - 20, 20)
    }

    private fun validateStatistics(cache: TqCache<*, *>, hits: Int, misses: Int) {
        val stats = cache.stats
        assertEquals(hits, stats.hits)
        assertEquals(misses, stats.misses)
    }
}