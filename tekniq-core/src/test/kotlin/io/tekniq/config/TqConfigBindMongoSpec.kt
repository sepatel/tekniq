package io.tekniq.config

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

object TqConfigBindMongoSpec : DescribeSpec({
    describe("MongoConfig from .properties (proves nested + List + Map)") {
        it("binds MongoConfig with all complex shapes from flat properties keys") {
            val config = TqPropertiesConfig(
                "classpath:/io/tekniq/config/mongo.properties", stopOnFailure = false
            )
            val bound = config.bind<MongoConfig>()
            bound shouldBe MongoConfig(
                uri = "mongodb://user:pass@host:27017",
                database = "orders",
                pool = MongoPoolConfig(min = 2, max = 20, acquireTimeoutMs = 45000),
                retry = MongoRetryConfig(attempts = 5, backoffMs = 250),
                ssl = MongoSslConfig(enabled = true, trustStorePath = "/etc/ssl/mongo.jks"),
                options = mapOf("timeout" to "5000", "appName" to "order-service"),
                tags = listOf("primary", "replica")
            )
        }
    }
})
