package io.tekniq.schedule

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*
import kotlin.test.assertEquals

object TqCronSpek : Spek({
    val base = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2017)
        set(Calendar.MONTH, Calendar.APRIL)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    describe("basic pattern") {
        it("triggers every 10th second") {
            val cron = TqCron("10 * * * * *")
            var now = base
            IntRange(1, 8).forEach {
                now = cron.next(now)
                assertEquals(10000L + ((it - 1) * 60000L), now.time - base.time)
            }
        }

        it("triggers every 10 seconds") {
            val cron = TqCron("*/10 * * * * *")
            var now = base
            IntRange(1, 8).forEach {
                now = cron.next(now)
                assertEquals(10000L + ((it - 1) * 10000L), now.time - base.time)
            }
        }

        it("triggers at noon daily") {
            val cron = TqCron("0 0 12 * * *")
            var now = base

            IntRange(1, 8).forEach {
                now = cron.next(now)
                assertEquals(60000L * 60 * 12 + ((it - 1) * 60000L * 60 * 24), now.time - base.time)
            }
        }
    }
})
