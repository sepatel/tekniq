package io.tekniq.config

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

open class TqYamlConfig(private val yamlFile: String, private val stopOnFailure: Boolean = true) : TqMapConfig(loadYaml(yamlFile, stopOnFailure)) {
    companion object {
        private val yaml = Yaml()
        
        @Suppress("UNCHECKED_CAST")
        private fun loadYaml(yamlFile: String, stopOnFailure: Boolean): Map<String, Any> {
            return try {
                val inputStream: InputStream? = if (yamlFile.startsWith("classpath:")) {
                    val filename = yamlFile.substring("classpath:".length)
                    TqYamlConfig::class.java.getResourceAsStream(filename)
                        ?: TqYamlConfig::class.java.classLoader.getResourceAsStream(filename)
                        ?: if (stopOnFailure) throw FileNotFoundException("Resource $yamlFile could not be read") else null
                } else {
                    File(yamlFile).inputStream()
                }
                if (inputStream == null) {
                    emptyMap()
                } else {
                    val loaded: Any? = yaml.load(inputStream)
                    if (loaded is Map<*, *>) {
                        loaded as Map<String, Any>
                    } else {
                        throw IllegalStateException("YAML file must contain a root map")
                    }
                }
            } catch (e: Exception) {
                if (stopOnFailure) throw e
                emptyMap()
            }
        }
    }

    override fun reload() {
        reload(loadYaml(yamlFile, stopOnFailure))
    }
}