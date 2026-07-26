package io.tekniq.config

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

open class TqYamlConfig(private val yamlFile: String, private val stopOnFailure: Boolean = true) :
    TqMapConfig(loadYaml(yamlFile, stopOnFailure).orEmpty()) {

    companion object {
        /** Null means the source could not be read; the caller decides whether that is fatal. */
        @Suppress("UNCHECKED_CAST")
        private fun loadYaml(yamlFile: String, stopOnFailure: Boolean): Map<String, Any>? = try {
            val source = openSource(yamlFile)
                ?: throw FileNotFoundException("Resource $yamlFile could not be read")
            // SnakeYAML's Yaml is documented as not thread safe, so it cannot be shared; under
            // concurrent loads a shared instance throws from inside its own parser state.
            when (val loaded = source.use { Yaml().load<Any?>(it) }) {
                null -> emptyMap() // a document with no content is an empty config, not a failure
                is Map<*, *> -> loaded as Map<String, Any>
                else -> error("YAML file must contain a root map")
            }
        } catch (e: Exception) {
            if (stopOnFailure) throw e
            null
        }

        private fun openSource(yamlFile: String): InputStream? = if (yamlFile.startsWith("classpath:")) {
            val filename = yamlFile.removePrefix("classpath:")
            TqYamlConfig::class.java.getResourceAsStream(filename)
                ?: TqYamlConfig::class.java.classLoader.getResourceAsStream(filename)
        } else {
            File(yamlFile).inputStream()
        }
    }

    // A reload that cannot read the file leaves the previous values in place. Replacing them with an
    // empty map would make every key vanish, so merely saving a half-written file wiped the config.
    override fun reload() {
        loadYaml(yamlFile, stopOnFailure)?.let { reload(it) }
    }
}