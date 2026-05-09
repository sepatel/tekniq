package io.tekniq.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileNotFoundException

open class TqJsonConfig(private val jsonFile: String, private val stopOnFailure: Boolean = true) : TqMapConfig(loadJson(jsonFile, stopOnFailure)) {
    companion object {
        private val mapper = ObjectMapper()
        
        @Suppress("UNCHECKED_CAST")
        private fun loadJson(jsonFile: String, stopOnFailure: Boolean): Map<String, Any> {
            return try {
                val inputStream = if (jsonFile.startsWith("classpath:")) {
                    val filename = jsonFile.substring("classpath:".length)
                    TqJsonConfig::class.java.getResourceAsStream(filename)
                        ?: TqJsonConfig::class.java.classLoader.getResourceAsStream(filename)
                        ?: if (stopOnFailure) throw FileNotFoundException("Resource $jsonFile could not be read") else null
                } else {
                    File(jsonFile).inputStream()
                }
                inputStream?.let { mapper.readValue(it, object : TypeReference<Map<String, Any>>() {}) }
                    ?: emptyMap()
            } catch (e: Exception) {
                if (stopOnFailure) throw e
                emptyMap()
            }
        }
    }

    override fun reload() {
        reload(loadJson(jsonFile, stopOnFailure))
    }
}