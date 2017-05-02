package io.tekniq.jdbc

import org.junit.Assert.*
import org.junit.Test
import java.sql.ResultSet

data class FooRow(val id: Int, val name: String)

class TqConnectionExtKtTest {
    companion object {
        val subject = TqSingleConnectionDataSource("jdbc:hsqldb:mem:tekniq", "sa", "").connection.apply {
            val stmt = createStatement()
            stmt.execute("DROP TABLE spektest IF EXISTS ")
            stmt.execute("CREATE TABLE spektest ( id INTEGER , name VARCHAR(100) )")
            stmt.execute("INSERT INTO spektest(id, name) VALUES(1, 'Foo')")
            stmt.close()
        }

        val mapper: ResultSet.() -> FooRow = {
            FooRow(getInt("id"), getString("name"))
        }
    }

    @Test fun usingAnOpenAndValidDbConnection() {
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
            assertEquals(42, result?.id)
            assertEquals("Meaning of Life", result?.name)
        }

        run {
            //can return null when selecting a non-existing record
            val result = subject.selectOne(sql, 69, action = mapper)
            assertNull(result)
        }

        run {
            //can efficiently act upon multiple rows of data without mapping and memory overhead
            val sql = "SELECT id, name FROM spektest"
            val x = subject.select(sql) {
                // returns a Unit not a FooRow
                FooRow(getInt("id"), getString("name"))
            }
            val y = subject.select<FooRow>(sql) {
                // returns a List<FooRow>
                FooRow(getInt("id"), getString("name"))
            }
            assertNotEquals(x, y)

            val z = subject.select(sql) {
                // returns a Unit not a FooRow
                FooRow(getInt("id"), getString("name"))
            }
            assertEquals(x, z)
            assertNotEquals(y, z)

            val w = subject.select(sql, action = mapper) // mapper explicitly returns a FooRow
            assertEquals(y, w)
            assertNotEquals(x, w)
        }
    }
}

