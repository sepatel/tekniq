package io.tekniq.jdbc

import io.kotest.core.spec.style.DescribeSpec
import kotlin.test.assertEquals

object TqResultSetExtKtSpec : DescribeSpec({
    val subject = TqSingleConnectionDataSource("jdbc:hsqldb:mem:tekniq", "sa", "").connection.apply {
        val stmt = createStatement()
        stmt.execute("DROP TABLE spekresult IF EXISTS ")
        stmt.execute("CREATE TABLE spekresult(id INTEGER, bool BOOLEAN, b TINYINT, small SMALLINT, number INTEGER, large BIGINT, f FLOAT, d DOUBLE)")
        stmt.execute("INSERT INTO spekresult VALUES(1, true, 12, 131, 4096, ${Long.MAX_VALUE}, 3.14, ${Double.MAX_VALUE})")
        stmt.execute("INSERT INTO spekresult VALUES(2, NULL, NULL, NULL, NULL, NULL, NULL, NULL)")
        stmt.close()
    }

    describe("basic testing") {
        it("using an open and valid db connection") {
            val sql = "SELECT * FROM spekresult WHERE id=?"
            run {
                // can read non-null values correctly
                subject.selectOne(sql, 1) { rs ->
                    assertEquals(1, rs.getInt("id"))
                    assertEquals(Double.MAX_VALUE, rs.getDoubleNull(8)!!, 0.1)
                    assertEquals(Double.MAX_VALUE, rs.getDoubleNull("d")!!, 0.1)
                    assertEquals(3.14f, rs.getFloatNull(7)!!, 0.1f)
                    assertEquals(3.14f, rs.getFloatNull("f")!!, 0.1f)
                    assertEquals(Long.MAX_VALUE, rs.getLongNull(6))
                    assertEquals(Long.MAX_VALUE, rs.getLongNull("large"))
                    assertEquals(4096, rs.getIntNull(5))
                    assertEquals(4096, rs.getIntNull("number"))
                    assertEquals(131.toShort(), rs.getShortNull(4))
                    assertEquals(131.toShort(), rs.getShortNull("small"))
                    assertEquals(12.toByte(), rs.getByteNull(3))
                    assertEquals(12.toByte(), rs.getByteNull("b"))
                    assertEquals(true, rs.getBooleanNull(2))
                    assertEquals(true, rs.getBooleanNull("bool"))
                }
            }
        }

        it("using cached results") {
            val sql = "SELECT * FROM spekresult WHERE id=?"
            run {
                // can read non-null values correctly
                subject.select(sql, 1).forEach { rs ->
                    assertEquals(1, rs.getInt("id"))
                    assertEquals(Double.MAX_VALUE, rs.getDoubleNull(8)!!, 0.1)
                    assertEquals(Double.MAX_VALUE, rs.getDoubleNull("d")!!, 0.1)
                    assertEquals(3.14f, rs.getFloatNull(7)!!, 0.1f)
                    assertEquals(3.14f, rs.getFloatNull("f")!!, 0.1f)
                    assertEquals(Long.MAX_VALUE, rs.getLongNull(6))
                    assertEquals(Long.MAX_VALUE, rs.getLongNull("large"))
                    assertEquals(4096, rs.getIntNull(5))
                    assertEquals(4096, rs.getIntNull("number"))
                    assertEquals(131.toShort(), rs.getShortNull(4))
                    assertEquals(131.toShort(), rs.getShortNull("small"))
                    assertEquals(12.toByte(), rs.getByteNull(3))
                    assertEquals(12.toByte(), rs.getByteNull("b"))
                    assertEquals(true, rs.getBooleanNull(2))
                    assertEquals(true, rs.getBooleanNull("bool"))
                }
            }
        }
    }
})
