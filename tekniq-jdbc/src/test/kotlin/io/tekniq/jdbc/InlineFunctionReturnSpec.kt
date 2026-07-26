package io.tekniq.jdbc

import io.kotest.core.spec.style.DescribeSpec
import kotlin.test.assertEquals

object InlineFunctionReturnSpec : DescribeSpec({
    val conn = TqSingleConnectionDataSource("jdbc:hsqldb:mem:tekniq", "sa", "").connection.apply {
        val stmt = createStatement()
        stmt.execute("DROP TABLE dataone IF EXISTS")
        stmt.execute("CREATE TABLE dataone(id INTEGER, s VARCHAR(20))")
        stmt.execute("INSERT INTO dataone VALUES(1, 'Pi')")
        stmt.execute("INSERT INTO dataone VALUES(2, NULL)")
        stmt.execute("INSERT INTO dataone VALUES(3, 'Light')")

        stmt.execute("DROP TABLE dataoption IF EXISTS")
        stmt.execute("CREATE TABLE dataoption(dataone_id INTEGER, color VARCHAR(20))")
        stmt.execute("INSERT INTO dataoption VALUES(1, 'Transparent')")
        stmt.execute("INSERT INTO dataoption VALUES(3, 'Darkness')")
        stmt.close()
    }

    describe("iteration over nested selects") {
        it("can use labeled return to break out of nested iteration") {
            var answer = "wrong"
            outerLoop@ for (row in conn.select("SELECT * from (VALUES(0))") { getInt(1) }) {
                for (dataRow in conn.select("SELECT id, s FROM dataone") { Pair(getInt("id"), getString("s")) }) {
                    val color = conn.select("SELECT color FROM dataoption WHERE dataone_id=?", dataRow.first) { getString("color") }.firstOrNull()
                    if (color == "Transparent") {
                        answer = dataRow.second
                        break@outerLoop
                    }
                    answer = "reallyWrong"
                }
                answer = "howSoWrong"
            }
            assertEquals("Pi", answer)
        }
    }
})