package io.tekniq.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

object TqConfigBindJacksonSpec : DescribeSpec({
    val mapper = jacksonObjectMapper()

    describe("bindJackson delegates to bind() and round-trips through Jackson") {
        it("binds MongoConfigJackson from .json (properties + nested + map + list)") {
            val config = TqJsonConfig("classpath:/io/tekniq/config/mongo.json", stopOnFailure = false)
            val bound = config.bindJackson<MongoConfigJackson>(mapper, prefix = "mongo")
            bound.uri shouldBe "mongodb://user:pass@host:27017"
            bound.database shouldBe "orders"
            bound.pool shouldBe MongoPoolConfigJackson(min = 2, max = 20)
            bound.options shouldBe mapOf("timeout" to "5000", "appName" to "order-service")
        }

        it("round-trips through Jackson and emits @JsonInclude(NON_NULL)") {
            val config = TqJsonConfig("classpath:/io/tekniq/config/mongo.json", stopOnFailure = false)
            val bound = config.bindJackson<MongoConfigJackson>(mapper, prefix = "mongo")
            val json = mapper.writeValueAsString(bound)
            json.contains("\"debugInfo\"") shouldBe false
        }

        it("works with a custom-prefix data class (no @TqConfigPrefix needed)") {
            val config = TqMapConfig(
                mapOf(
                    "app.uri" to "mongodb://flat",
                    "app.database" to "flat-db",
                    "app.pool.min" to "3",
                    "app.pool.max" to "10"
                )
            )
            val bound = config.bindJackson<MongoConfigJackson>(mapper, prefix = "app")
            bound.uri shouldBe "mongodb://flat"
            bound.database shouldBe "flat-db"
            bound.pool shouldBe MongoPoolConfigJackson(min = 3, max = 10)
        }

        it("uses Jackson defaults — accepts a Map<String, String> as Map<String, V> even when values are objects") {
            val config = TqMapConfig(
                mapOf(
                    "mongo.uri" to "mongodb://x",
                    "mongo.database" to "y",
                    "mongo.pool.min" to "1",
                    "mongo.pool.max" to "2"
                )
            )
            val bound = config.bindJackson<MongoConfigJackson>(mapper, prefix = "mongo")
            bound.pool shouldBe MongoPoolConfigJackson(min = 1, max = 2)
        }
    }
})
