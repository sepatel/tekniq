package io.tekniq.schedule

import org.junit.Test

import org.junit.Assert.*
import java.util.*

class TqCronTest {
    private val base = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2017)
        set(Calendar.MONTH, Calendar.APRIL)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    @Test fun every10thSecond() {
        val cron = TqCron("10 * * * * *")
        var now = base
        IntRange(1, 8).forEach {
            now = cron.next(now)
            assertEquals(10000L + ((it - 1) * 60000L), now.time - base.time)
        }
    }

    @Test fun every10Seconds() {
        val cron = TqCron("*/10 * * * * *")
        var now = base
        IntRange(1, 8).forEach {
            now = cron.next(now)
            assertEquals(10000L + ((it - 1) * 10000L), now.time - base.time)
        }
    }

    @Test fun everyDayAtNoon() {
        val cron = TqCron("0 0 12 * * *")
        var now = base

        IntRange(1, 8).forEach {
            now = cron.next(now)
            assertEquals(60000L * 60 * 12 + ((it - 1) * 60000L * 60 * 24), now.time - base.time)
        }
    }
}