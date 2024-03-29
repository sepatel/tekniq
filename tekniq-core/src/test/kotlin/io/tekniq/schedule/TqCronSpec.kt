package io.tekniq.schedule

import io.kotest.core.spec.style.DescribeSpec
import java.util.*
import kotlin.test.assertEquals

object TqCronSpec : DescribeSpec({
    val base = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2017)
        set(Calendar.MONTH, Calendar.JULY)
        set(Calendar.DAY_OF_MONTH, 3)
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
            val base = cron.next(base) // wind up to the next noon marker whether it be today or tomorrow
            var now = base
            IntRange(1, 8).forEach {
                now = cron.next(now)
                assertEquals(it * 1000 * 60 * 60 * 24L, now.time - base.time)
            }
        }
    }
})
