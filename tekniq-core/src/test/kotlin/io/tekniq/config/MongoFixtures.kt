package io.tekniq.config

@TqConfigPrefix("mongo")
data class MongoConfig(
    val uri: String,
    val database: String,
    val pool: MongoPoolConfig = MongoPoolConfig(),
    val retry: MongoRetryConfig = MongoRetryConfig(),
    val ssl: MongoSslConfig? = null,
    val options: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList()
)

data class MongoPoolConfig(
    val min: Int = 0,
    val max: Int = 100,
    val acquireTimeoutMs: Long = 30_000
)

data class MongoRetryConfig(
    val attempts: Int = 3,
    val backoffMs: Long = 100
)

data class MongoSslConfig(
    val enabled: Boolean = false,
    val trustStorePath: String? = null
)
