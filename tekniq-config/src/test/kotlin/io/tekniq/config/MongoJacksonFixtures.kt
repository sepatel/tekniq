package io.tekniq.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
data class MongoConfigJackson(
    @JsonProperty("uri") val uri: String,
    @JsonProperty("database") val database: String,
    @JsonProperty("pool") val pool: MongoPoolConfigJackson = MongoPoolConfigJackson(),
    @JsonProperty("options") val options: Map<String, String> = emptyMap(),
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("debugInfo") val debugInfo: String? = null
)

data class MongoPoolConfigJackson(
    @JsonProperty("min") val min: Int = 0,
    @JsonProperty("max") val max: Int = 100,
    @JsonIgnore val reservedField: String = ""
)
