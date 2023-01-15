package io.tekniq.config

import java.io.File
import java.io.FileNotFoundException
import java.util.*

open class TqPropertiesConfig(private val propertiesFile: String, private val stopOnFailure: Boolean = true) :
    TqMapConfig(loadProperties(propertiesFile, stopOnFailure)) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun loadProperties(propertiesFile: String, stopOnFailure: Boolean) = Properties().apply {
            if (propertiesFile.startsWith("classpath:")) {
                val filename = propertiesFile.substring("classpath:".length)
                val stream = javaClass.getResourceAsStream(filename)
                    ?: Properties::javaClass.javaClass.getResourceAsStream(filename)
                    ?: TqPropertiesConfig::class.java.classLoader.getResourceAsStream(filename)
                if (stream != null) {
                    load(stream)
                } else if (stopOnFailure) {
                    throw FileNotFoundException("Resource $propertiesFile could not be read")
                }
            } else {
                try {
                    load(File(propertiesFile).inputStream())
                } catch (@SuppressWarnings("TooGenericExceptionCaught") e: Exception) {
                    if (stopOnFailure) throw e
                }
            }
        } as Map<String, Any>
    }

    override fun reload() = reload(loadProperties(propertiesFile, stopOnFailure))
}
