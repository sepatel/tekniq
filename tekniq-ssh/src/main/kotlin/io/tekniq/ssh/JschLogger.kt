package io.tekniq.ssh

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Logger
import org.slf4j.LoggerFactory

object JschLogger : Logger {
    private val logger = LoggerFactory.getLogger(JschLogger::class.java)

    fun init() {
        JSch.setLogger(JschLogger)
    }

    override fun isEnabled(level: Int): Boolean = true

    override fun log(level: Int, message: String?) {
        when (level) {
            Logger.DEBUG -> logger.debug(message)
            Logger.INFO -> logger.info(message)
            Logger.WARN -> logger.warn(message)
            Logger.ERROR -> logger.error(message)
            Logger.FATAL -> logger.error(message)
            else -> logger.warn("Invalid logging level $level")
        }
    }
}
