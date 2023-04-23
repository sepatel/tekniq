package io.tekniq.jdbc

import io.kotest.core.spec.style.DescribeSpec
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.*

data class FooRow(val id: Int, val name: String)

object TqConnectionExtKtSpek : DescribeSpec({
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
            run {
                val result = subject.selectOne("SELECT * FROM issue01 WHERE id=1") { rs ->
                    Triple(rs.getDate("date"), rs.getTime("time"), rs.getTimestamp("ts"))
                } ?: fail("Expected row back")
                assertEquals(localDate, result.first.toLocalDate())
                assertEquals(localTime, result.second.toLocalTime())
                assertEquals(localDateTime, result.third.toLocalDateTime())
            }
        }
    }

    describe("basic connection validation") {
        it("using an open and valid db connection") {
            val sql = "SELECT id, name FROM spektest WHERE id=?"
            run {
                // can read an existing record in a table
                val result = subject.selectOne(sql, 1, action = mapper)!!
                assertNotNull(result)
                assertEquals(1, result.id)
                assertEquals("Foo", result.name)
            }

            run {
                //can add 1 row and read the written record
                val rows = subject.insert("INSERT INTO spektest(id, name) VALUES(?, ?)", 42, "Meaning of Life")
                assertEquals(1, rows)
                assertEquals(2, subject.select("SELECT id, name FROM spektest", action = mapper).size)
                val result = subject.selectOne(sql, 42, action = mapper)
                assertNotNull(result)
                assertEquals(42, result.id)
                assertEquals("Meaning of Life", result.name)
            }

            run {
                //can return null when selecting a non-existing record
                val result = subject.selectOne(sql, 69, action = mapper)
                assertNull(result)
            }

            run {
                //can efficiently act upon multiple rows of data without mapping and memory overhead
                val sql = "SELECT id, name FROM spektest"
                val x = subject.select(sql) { rs ->
                    // returns a Unit not a FooRow
                    FooRow(rs.getInt("id"), rs.getString("name"))
                }
                val y = subject.select<FooRow>(sql) {
                    // returns a List<FooRow>
                    FooRow(it.getInt("id"), it.getString("name"))
                }
                assertNotEquals<Any>(x, y)

                val z = subject.select(sql) {
                    // returns a Unit not a FooRow
                    FooRow(it.getInt("id"), it.getString("name"))
                }
                assertEquals(x, z)
                assertNotEquals<Any>(y, z)

                val w = subject.select(sql, action = mapper) // mapper explicitly returns a FooRow
                assertEquals(y, w)
                assertNotEquals<Any>(x, w)
            }
        }

        it("using cached row sets") {
            subject.select("SELECT * from spektest")
                .also { assertTrue(it.size() == 2, "Size is ${it.size()}") }
                .forEach {
                    val row = mapper(it)
                    assertEquals(it.getInt(1), row.id)
                }
        }
    }
})
