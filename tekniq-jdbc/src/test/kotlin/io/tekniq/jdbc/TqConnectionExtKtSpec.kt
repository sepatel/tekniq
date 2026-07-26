package io.tekniq.jdbc

import io.kotest.core.spec.style.DescribeSpec
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.*

data class FooRow(val id: Int, val name: String)

object TqConnectionExtKtSpec : DescribeSpec({
    val subject = TqSingleConnectionDataSource("jdbc:hsqldb:mem:tekniq", "sa", "").connection.apply {
        val stmt = createStatement()
        stmt.execute("DROP TABLE spektest IF EXISTS ")
        stmt.execute("CREATE TABLE spektest ( id INTEGER , name VARCHAR(100) )")
        stmt.execute("INSERT INTO spektest(id, name) VALUES(1, 'Foo')")
        stmt.execute("CREATE TABLE issue01(id INTEGER, date DATE, time TIME, ts DATETIME)")
        stmt.close()
    }

    val mapper: ResultSet.() -> FooRow = {
        FooRow(getInt("id"), getString("name"))
    }

    describe("Issue 1: https://github.com/sepatel/tekniq/issues/1") {
        it("read date/time correctly") {
            val localDate = LocalDate.now()
            val localTime = LocalTime.ofSecondOfDay(7777)
            val localDateTime = LocalDateTime.of(localDate, localTime)
            subject.insert("INSERT INTO issue01 VALUES(1, ?, ?, ?)", localDate, localTime, localDateTime)
            val result = subject.selectFirst("SELECT * FROM issue01 WHERE id=1") {
                Triple(getDate("date"), getTime("time"), getTimestamp("ts"))
            } ?: fail("Expected row back")
            assertEquals(localDate, result.first.toLocalDate())
            assertEquals(localTime, result.second.toLocalTime())
            assertEquals(localDateTime, result.third.toLocalDateTime())
        }
    }

    describe("basic connection validation") {
        it("using an open and valid db connection") {
            val result = subject.selectFirst("SELECT id, name FROM spektest WHERE id=?", 1, action = mapper)!!
            assertNotNull(result)
            assertEquals(1, result.id)
            assertEquals("Foo", result.name)
        }

        it("can add 1 row and read the written record") {
            val rows = subject.insert("INSERT INTO spektest(id, name) VALUES(?, ?)", 42, "Meaning of Life")
            assertEquals(1, rows)
            assertEquals(2, subject.select("SELECT id, name FROM spektest", action = mapper).toList().size)
            val result = subject.selectFirst("SELECT id, name FROM spektest WHERE id=?", 42, action = mapper)
            assertNotNull(result)
            assertEquals(42, result.id)
            assertEquals("Meaning of Life", result.name)
        }

        it("can return null when selecting a non-existing record") {
            val result = subject.selectFirst("SELECT id, name FROM spektest WHERE id=?", 69, action = mapper)
            assertNull(result)
        }

        it("can stream multiple rows") {
            val results = subject.select("SELECT id, name FROM spektest", action = mapper).toList()
            assertEquals(2, results.size)
            assertEquals(1, results[0].id)
            assertEquals("Foo", results[0].name)
            assertEquals(42, results[1].id)
            assertEquals("Meaning of Life", results[1].name)
        }
    }

    describe("using cached row sets") {
        it("can use cached row set") {
            subject.select("SELECT * from spektest")
                .also { assertEquals(2, it.size()) }
                .forEach {
                    val id = getInt(1)
                    assertTrue(id == 1 || id == 42)
                }
        }
    }

    describe("named parameters") {
        it("can use named parameters") {
            val result = subject.selectFirst(
                "SELECT id, name FROM spektest WHERE id = :id",
                mapOf("id" to 1),
                action = { FooRow(getInt("id"), getString("name")) }
            )
            assertNotNull(result)
            assertEquals(1, result.id)
            assertEquals("Foo", result.name)
        }
    }
})