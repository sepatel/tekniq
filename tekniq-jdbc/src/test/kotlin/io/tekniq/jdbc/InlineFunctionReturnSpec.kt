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

    describe("failure within nested selects") {
        it("breaks correct within the nesting") {
            var answer = "wrong"
            conn.select("SELECT * from (VALUES(0))") outerLoop@{
                conn.select("SELECT id, s FROM dataone") {
                    val s = it.getString("s")
                    conn.select("SELECT color FROM dataoption WHERE dataone_id=?", it.getInt("id")) {
                        if (it.getString("color") == "Transparent") {
                            answer = s
                            return@outerLoop
                        }
                        answer = "reallyWrong"
                    }
                    answer = "awesomeWrong"
                }
                answer = "howSoWrong"
            }
            assertEquals("Pi", answer)
        }
    }
})
