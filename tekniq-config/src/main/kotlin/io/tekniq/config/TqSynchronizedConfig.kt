package io.tekniq.config

import java.util.concurrent.locks.ReentrantReadWriteLock

class TqSynchronizedConfig(private val delegate: TqConfig) : TqConfig() {
    private val lock = ReentrantReadWriteLock()

    override fun <T> getValue(key: String, type: Class<T>?): T? = lock.readLock().let {
        it.lock()
        try {
            delegate.getValue(key, type)
        } finally {
            it.unlock()
        }
    }

    override fun contains(key: String): Boolean = lock.readLock().let {
        it.lock()
        try {
            delegate.contains(key)
        } finally {
            it.unlock()
        }
    }

    override fun reload() = lock.writeLock().let {
        it.lock()
        try {
            delegate.reload()
        } finally {
            it.unlock()
        }
    }

    override fun reload(newConfigs: Map<String, Any?>) = lock.writeLock().let {
        it.lock()
        try {
            delegate.reload(newConfigs)
        } finally {
            it.unlock()
        }
    }
}