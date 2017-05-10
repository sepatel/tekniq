package io.tekniq.cache

import org.junit.Assert.*
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
        1.rangeTo(20).forEach { i ->
            // spawn 20 threads
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
        1.rangeTo(20).forEach { i ->
            // spawn 20 threads
            threads.add(thread {
                1.rangeTo(100).forEach { x ->
                    // each thread hits 100 times
                    val answer = cache[i.toString()]
                    assertEquals(i, answer) // seed the cache
                }
            })
        }

        threads.forEach(Thread::join)
        validateStatistics(cache, 20 * 100 - 20, 20)
    }

    @Test fun cacheAllowsNullValues() {
        val cache = TqCache<Int, String?>(recordStats = true) {
            when (it % 2 == 0) {
                true -> it.toString()
                else -> null
            }
        }

        validateStatistics(cache, 0, 0)
        assertEquals("4", cache[4])
        validateStatistics(cache, 0, 1)
        assertEquals("4", cache[4])
        validateStatistics(cache, 1, 1)
        assertNull(cache[3])
        validateStatistics(cache, 1, 2)
        assertNull(cache[3])
        validateStatistics(cache, 2, 2)
    }

    @Test fun maxSizeVerification() {
        val cache = TqCache<Int, Int>(maximumSize = 5, recordStats = true) { it * it }
        validateStatistics(cache, 0, 0)
        1.rangeTo(5).forEach {
            assertEquals(false, cache.containsKey(it))
            assertEquals(it * it, cache[it])
            assertEquals(true, cache.containsKey(it))
        }
        assertTrue(cache.containsKey(1))
        assertTrue(cache.containsKey(2))
        assertTrue(cache.containsKey(3))
        assertTrue(cache.containsKey(4))
        assertTrue(cache.containsKey(5))
        assertFalse(cache.containsKey(6))

        3.rangeTo(8).forEach { // 3, 4, and 5 stay, 6 erases 1, 7 erases 2, 8 erases 3
            assertEquals(it * it, cache[it])
            assertEquals(true, cache.containsKey(it))
        }

        assertFalse(cache.containsKey(1))
        assertFalse(cache.containsKey(2))
        assertFalse(cache.containsKey(3))
        assertTrue(cache.containsKey(4))
        assertTrue(cache.containsKey(5))
        assertTrue(cache.containsKey(6))
        assertTrue(cache.containsKey(7))
        assertTrue(cache.containsKey(8))

        1.rangeTo(4).forEach { assertEquals(it * it, cache[it]) } // 1 erases 4, 2 erases 5, 3 erases 6, 4 erases 7

        assertTrue(cache.containsKey(1))
        assertTrue(cache.containsKey(2))
        assertTrue(cache.containsKey(3))
        assertTrue(cache.containsKey(4))
        assertFalse(cache.containsKey(5))
        assertFalse(cache.containsKey(6))
        assertFalse(cache.containsKey(7))
        assertTrue(cache.containsKey(8))

        8.rangeTo(10).forEach { assertEquals(it * it, cache[it]) } // 8 stays, 9 erases 8, 10 erases 1

        assertFalse(cache.containsKey(7))
        assertFalse(cache.containsKey(8))
        assertTrue(cache.containsKey(9))
        assertTrue(cache.containsKey(10))
    }

    private fun validateStatistics(cache: TqCache<*, *>, hits: Int, misses: Int) {
        val stats = cache.stats
        assertEquals(hits, stats.hits)
        assertEquals(misses, stats.misses)
    }
}