package io.tekniq.config

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TqWatchedConfig(
    private val config: TqConfig,
    private val configFilePath: String,
    private val pollIntervalMs: Long = 1000L
) {
    private var executor: ExecutorService? = null
    private var watcher: WatchService? = null

    fun startWatching() {
        val path = java.nio.file.FileSystems.getDefault().getPath(configFilePath).let {
            if (configFilePath.startsWith("classpath:")) null else it.parent
        }

        executor = Executors.newSingleThreadExecutor { r -> Thread(r, "config-watcher").apply { isDaemon = true } }
        watcher = FileSystems.getDefault().newWatchService()

        executor?.execute {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val watchKey = watcher?.take() ?: break
                    for (event in watchKey.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            config.reload()
                        }
                    }
                    watchKey.reset()
                }
            } catch (e: InterruptedException) {
                // Expected on shutdown
            }
        }

        path?.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)
    }

    fun stopWatching() {
        executor?.shutdownNow()
        watcher?.close()
    }
}