package io.tekniq.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileNotFoundException

open class TqJsonConfig(private val jsonFile: String, private val stopOnFailure: Boolean = true) :
    TqMapConfig(loadJson(jsonFile, stopOnFailure).orEmpty()) {

    companion object {
        private val mapper = ObjectMapper()

        /** Null means the source could not be read; the caller decides whether that is fatal. */
        private fun loadJson(jsonFile: String, stopOnFailure: Boolean): Map<String, Any>? = try {
            openSource(jsonFile)?.use { mapper.readValue(it, object : TypeReference<Map<String, Any>>() {}) }
                ?: if (stopOnFailure) throw FileNotFoundException("Resource $jsonFile could not be read") else null
        } catch (e: Exception) {
            if (stopOnFailure) throw e
            null
        }

        private fun openSource(jsonFile: String) = if (jsonFile.startsWith("classpath:")) {
            val filename = jsonFile.removePrefix("classpath:")
            TqJsonConfig::class.java.getResourceAsStream(filename)
                ?: TqJsonConfig::class.java.classLoader.getResourceAsStream(filename)
        } else {
            File(jsonFile).inputStream()
        }
    }

    // A reload that cannot read the file leaves the previous values in place. Replacing them with an
    // empty map would make every key vanish, so merely saving a half-written file wiped the config.
    override fun reload() {
        loadJson(jsonFile, stopOnFailure)?.let { reload(it) }
    }
}