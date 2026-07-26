package io.tekniq.config

import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

open class TqPropertiesConfig(private val propertiesFile: String, private val stopOnFailure: Boolean = true) :
    TqMapConfig(loadProperties(propertiesFile, stopOnFailure).orEmpty()) {

    companion object {
        /** Null means the source could not be read; the caller decides whether that is fatal. */
        @Suppress("UNCHECKED_CAST")
        private fun loadProperties(propertiesFile: String, stopOnFailure: Boolean): Map<String, Any>? = try {
            val source = openSource(propertiesFile)
                ?: throw FileNotFoundException("Resource $propertiesFile could not be read")
            source.use { stream -> Properties().apply { load(stream) } as Map<String, Any> }
        } catch (e: Exception) {
            if (stopOnFailure) throw e
            null
        }

        private fun openSource(propertiesFile: String): InputStream? =
            if (propertiesFile.startsWith("classpath:")) {
                val filename = propertiesFile.removePrefix("classpath:")
                TqPropertiesConfig::class.java.getResourceAsStream(filename)
                    ?: TqPropertiesConfig::class.java.classLoader.getResourceAsStream(filename)
            } else {
                File(propertiesFile).inputStream()
            }
    }

    // A reload that cannot read the file leaves the previous values in place. Replacing them with an
    // empty map would make every key vanish, so merely saving a half-written file wiped the config.
    override fun reload() {
        loadProperties(propertiesFile, stopOnFailure)?.let { reload(it) }
    }
}