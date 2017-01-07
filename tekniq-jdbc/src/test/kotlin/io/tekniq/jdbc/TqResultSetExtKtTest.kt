package io.tekniq.jdbc

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.assertEquals

class TqResultSetExtKtTest : Spek({
    val subject = TqSingleConnectionDataSource("jdbc:hsqldb:mem:tekniq", "sa", "").connection.apply {
        val stmt = createStatement()
        stmt.execute("DROP TABLE spekresult IF EXISTS ")
        stmt.execute("CREATE TABLE spekresult(id INTEGER, bool BOOLEAN, b TINYINT, small SMALLINT, number INTEGER, large BIGINT, f FLOAT, d DOUBLE)")
        stmt.execute("INSERT INTO spekresult VALUES(1, true, 12, 131, 4096, ${Long.MAX_VALUE}, 3.14, ${Double.MAX_VALUE})")
        stmt.execute("INSERT INTO spekresult VALUES(2, NULL, NULL, NULL, NULL, NULL, NULL, NULL)")
        stmt.close()
    }

    given("an open and valid db connection") {
        val sql = "SELECT * FROM spekresult WHERE id=?"
        it("can read non-null values correctly") {
            subject.selectOne(sql, 1) {
                assertEquals(1, getInt("id"))
                assertEquals(Double.MAX_VALUE, getDoubleNull(8)!!, 0.1)
                assertEquals(Double.MAX_VALUE, getDoubleNull("d")!!, 0.1)
                assertEquals(3.14f, getFloatNull(7)!!, 0.1f)
                assertEquals(3.14f, getFloatNull("f")!!, 0.1f)
                assertEquals(Long.MAX_VALUE, getLongNull(6))
                assertEquals(Long.MAX_VALUE, getLongNull("large"))
                assertEquals(4096, getIntNull(5))
                assertEquals(4096, getIntNull("number"))
                assertEquals(131.toShort(), getShortNull(4))
                assertEquals(131.toShort(), getShortNull("small"))
                assertEquals(12.toByte(), getByteNull(3))
                assertEquals(12.toByte(), getByteNull("b"))
                assertEquals(true, getBooleanNull(2))
                assertEquals(true, getBooleanNull("bool"))
            }
        }
    }
})

