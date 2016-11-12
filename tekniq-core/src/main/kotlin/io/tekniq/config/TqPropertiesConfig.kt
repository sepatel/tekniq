package io.tekniq.config

import java.io.File
import java.util.*

open class TqPropertiesConfig(private val propertiesFile: String, private val stopOnFailure: Boolean = true) : TqMapConfig(loadProperties(propertiesFile, stopOnFailure)) {
    override fun reload() = reload(loadProperties(propertiesFile, stopOnFailure))
}

private fun loadProperties(propertiesFile: String, stopOnFailure: Boolean) = Properties().apply {
    if (propertiesFile.startsWith("classpath:")) {
        load(javaClass.getResourceAsStream(propertiesFile.substring("classpath:".length)))
    } else {
        try {
            load(File(propertiesFile).inputStream())
        } catch (e: Exception) {
            if (stopOnFailure) {
                throw e
            }
        }
    }
} as Map<String, Any>
