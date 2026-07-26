package io.tekniq.config

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * Serialises reloads against reads so a reload is never observed half-applied.
 *
 * Every read delegates straight through rather than going via [TqConfig]'s memoisation. Using the
 * inherited cache would give the wrapper a private second copy of the values, which a reload on the
 * delegate cannot invalidate -- that is how this decorator used to serve stale values permanently.
 */
class TqSynchronizedConfig(private val delegate: TqConfig) : TqConfig() {
    private val lock = ReentrantReadWriteLock()

    override val keys: Set<String> get() = read { delegate.keys }

    override fun <T> get(key: String, type: Class<T>?): T? = read { delegate.get(key, type) }
    override fun <T> getValue(key: String, type: Class<T>?): T? = read { delegate.getValue(key, type) }
    override fun contains(key: String): Boolean = read { delegate.contains(key) }

    // Forwarded rather than inherited so a delegate that overrides its own coercion still wins.
    override fun getDouble(key: String): Double? = read { delegate.getDouble(key) }
    override fun getFloat(key: String): Float? = read { delegate.getFloat(key) }
    override fun getInt(key: String): Int? = read { delegate.getInt(key) }
    override fun getLong(key: String): Long? = read { delegate.getLong(key) }
    override fun getShort(key: String): Short? = read { delegate.getShort(key) }

    override fun reload() = write { delegate.reload() }
    override fun reload(newConfigs: Map<String, Any?>) = write { delegate.reload(newConfigs) }

    private inline fun <T> read(body: () -> T): T = lock.readLock().withLock(body)
    private inline fun <T> write(body: () -> T): T = lock.writeLock().withLock(body)
}
