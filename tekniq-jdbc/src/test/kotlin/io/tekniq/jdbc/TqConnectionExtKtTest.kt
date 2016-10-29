package io.tekniq.jdbc

import org.junit.Assert.*
import org.junit.Test
import java.sql.DriverManager
import java.sql.ResultSet

data class FooRow(val id: Int, val name: String)

class TqConnectionExtKtTest {
    private val conn = DriverManager.getConnection("jdbc:hsqldb:mem:tekniq", "sa", "").apply {
        autoCommit = true
        val stmt = createStatement()
        stmt.execute("DROP TABLE spektest IF EXISTS ")
        stmt.execute("CREATE TABLE spektest ( id INTEGER , name VARCHAR(100) )")
        stmt.execute("INSERT INTO spektest(id, name) VALUES(1, 'Foo')")
        stmt.close()
        commit()
    }

    @Test fun insertRecord() {
        val rows = conn.insert("INSERT INTO spektest(id, name) VALUES(42, 'Meaning of Life')")
        assertEquals(1, rows)
        assertEquals(2, conn.select<FooRow>("SELECT id, name FROM spektest") {
            FooRow(getInt("id"), getString("name"))
        }.size)
    }

    @Test fun selectOne() {
        val result = conn.selectOne("SELECT id, name FROM spektest") {
            FooRow(getInt("id"), getString("name"))
        }
        assertNotNull(result)
        assertEquals(1, result?.id)
        assertEquals("Foo", result?.name)
    }

    @Test fun selectWithNoReturn() {
        val action: ResultSet.() -> Unit = {
            FooRow(getInt("id"), getString("name"))
        }
        val sql = "SELECT id, name FROM spektest"
        val x = conn.select(sql, action = action)
        val y = conn.select<FooRow>(sql) {
            FooRow(getInt("id"), getString("name"))
        }
        assertFalse(x.equals(y))

        val z = conn.select(sql, action = action)
        assertTrue(x.equals(z))

        val w = conn.select<FooRow>(sql) {
            FooRow(getInt("id"), getString("name"))
        }
        assertTrue(y.equals(w))
    }
}

