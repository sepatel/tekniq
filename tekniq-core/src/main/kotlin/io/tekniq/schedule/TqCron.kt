package io.tekniq.schedule

import java.util.*

/**
 * This is a reimplementation of SpringFramework's CronSequenceGenerator.java code. It has been ported over to kotlin
 * code and most all of the original code and documentation is still in place from the original source implementation.
 *
 * Date sequence generator for a
 * <a href="http://www.manpagez.com/man/5/crontab/">Crontab pattern</a>,
 * allowing clients to specify a pattern that the sequence matches.
 *
 * <p>The pattern is a list of six single space-separated fields: representing
 * second, minute, hour, day, month, weekday. Month and weekday names can be
 * given as the first three letters of the English names.
 *
 * <p>Example patterns:
 * <ul>
 * <li>"0 0 * * * *" = the top of every hour of every day.</li>
 * <li>"*&#47;10 * * * * *" = every ten seconds.</li>
 * <li>"0 0 8-10 * * *" = 8, 9 and 10 o'clock of every day.</li>
 * <li>"0 0 6,19 * * *" = 6:00 AM and 7:00 PM every day.</li>
 * <li>"0 0/30 8-10 * * *" = 8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day.</li>
 * <li>"0 0 9-17 * * MON-FRI" = on the hour nine-to-five weekdays</li>
 * <li>"0 0 0 25 12 ?" = every Christmas Day at midnight</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sejal Patel
 */
class TqCron(val expression: String, val timeZone: TimeZone = TimeZone.getDefault()) {
    private val months = BitSet(12)
    private val daysOfMonth = BitSet(31)
    private val daysOfWeek = BitSet(7)
    private val hours = BitSet(24)
    private val minutes = BitSet(60)
    private val seconds = BitSet(60)

    init {
        val fields = expression.split(' ')
                .map(String::trim)
                .filterNot(String::isEmpty)
        if (!areValidCronFields(fields)) {
            throw IllegalArgumentException("Cron expression must consist of 6 fields (found ${fields.size} in \"$expression\")")
        }
        setNumberHits(this.seconds, fields[0], 0, 60)
        setNumberHits(this.minutes, fields[1], 0, 60)
        setNumberHits(this.hours, fields[2], 0, 24)
        setDaysOfMonth(this.daysOfMonth, fields[3])
        setMonths(this.months, fields[4])
        setDays(this.daysOfWeek, replaceOrdinals(fields[5], "SUN,MON,TUE,WED,THU,FRI,SAT"), 8)
        if (this.daysOfWeek.get(7)) {
            // Sunday can be represented as 0 or 7
            this.daysOfWeek.set(0)
            this.daysOfWeek.clear(7)
        }
    }

    /**
     * Get the next [Date] in the sequence matching the Cron pattern and
     * after the value provided. The return value will have a whole number of
     * seconds, and will be after the input value.
     * @param date a seed value
     * @return the next value matching the pattern
     */
    fun next(date: Date = Date()): Date {
        /*
		The plan:

		1 Start with whole second (rounding up if necessary)

		2 If seconds match move on, otherwise find the next match:
		2.1 If next match is in the next minute then roll forwards

		3 If minute matches move on, otherwise find the next match
		3.1 If next match is in the next hour then roll forwards
		3.2 Reset the seconds and go to 2

		4 If hour matches move on, otherwise find the next match
		4.1 If next match is in the next day then roll forwards,
		4.2 Reset the minutes and seconds and go to 2
		*/

        val calendar = GregorianCalendar()
        calendar.timeZone = this.timeZone
        calendar.time = date

        // First, just reset the milliseconds and try to calculate from there...
        calendar.set(Calendar.MILLISECOND, 0)
        val originalTimestamp = calendar.timeInMillis
        doNext(calendar, calendar.get(Calendar.YEAR))

        if (calendar.timeInMillis == originalTimestamp) {
            // We arrived at the original timestamp - round up to the next whole second and try again...
            calendar.add(Calendar.SECOND, 1)
            doNext(calendar, calendar.get(Calendar.YEAR))
        }

        return calendar.time
    }

    private fun doNext(calendar: Calendar, dot: Int) {
        val resets = ArrayList<Int>()

        val second = calendar.get(Calendar.SECOND)
        val emptyList = emptyList<Int>()
        val updateSecond = findNext(this.seconds, second, calendar, Calendar.SECOND, Calendar.MINUTE, emptyList)
        if (second == updateSecond) {
            resets.add(Calendar.SECOND)
        }

        val minute = calendar.get(Calendar.MINUTE)
        val updateMinute = findNext(this.minutes, minute, calendar, Calendar.MINUTE, Calendar.HOUR_OF_DAY, resets)
        if (minute == updateMinute) {
            resets.add(Calendar.MINUTE)
        } else {
            doNext(calendar, dot)
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val updateHour = findNext(this.hours, hour, calendar, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_WEEK, resets)
        if (hour == updateHour) {
            resets.add(Calendar.HOUR_OF_DAY)
        } else {
            doNext(calendar, dot)
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val updateDayOfMonth = findNextDay(calendar, this.daysOfMonth, dayOfMonth, daysOfWeek, dayOfWeek, resets)
        if (dayOfMonth == updateDayOfMonth) {
            resets.add(Calendar.DAY_OF_MONTH)
        } else {
            doNext(calendar, dot)
        }

        val month = calendar.get(Calendar.MONTH)
        val updateMonth = findNext(this.months, month, calendar, Calendar.MONTH, Calendar.YEAR, resets)
        if (month != updateMonth) {
            if (calendar.get(Calendar.YEAR) - dot > 4) {
                throw IllegalArgumentException("Invalid schedule expression \"$expression\" led to runaway search for next trigger")
            }
            doNext(calendar, dot)
        }
    }

    private fun findNextDay(calendar: Calendar, daysOfMonth: BitSet, dayOfMonth: Int, daysOfWeek: BitSet, dayOfWeek: Int,
                            resets: List<Int>): Int {
        var dayOfMonth = dayOfMonth
        var dayOfWeek = dayOfWeek

        var count = 0
        val max = 366
        // the DAY_OF_WEEK values in java.util.Calendar start with 1 (Sunday),
        // but in the schedule pattern, they start with 0, so we subtract 1 here
        while ((!daysOfMonth.get(dayOfMonth) || !daysOfWeek.get(dayOfWeek - 1)) && count++ < max) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            reset(calendar, resets)
        }
        if (count >= max) {
            throw IllegalArgumentException("Overflow in day for expression \"$expression\"")
        }
        return dayOfMonth
    }

    /**
     * Search the bits provided for the next set bit after the value provided, and reset the calendar.
     * @param bits a [BitSet] representing the allowed values of the field
     * @param value the current value of the field
     * @param calendar the calendar to increment as we move through the bits
     * @param field the field to increment in the calendar (@see [Calendar] for the static constants defining valid fields)
     * @param lowerOrders the Calendar field ids that should be reset (i.e. the ones of lower significance than the field of interest)
     * @return the value of the calendar field that is next in the sequence
     */
    private fun findNext(bits: BitSet, value: Int, calendar: Calendar, field: Int, nextField: Int, lowerOrders: List<Int>): Int {
        var nextValue = bits.nextSetBit(value)
        // roll over if needed
        if (nextValue == -1) {
            calendar.add(nextField, 1)
            reset(calendar, Arrays.asList(field))
            nextValue = bits.nextSetBit(0)
        }
        if (nextValue != value) {
            calendar.set(field, nextValue)
            reset(calendar, lowerOrders)
        }
        return nextValue
    }

    /**
     * Reset the calendar setting all the fields provided to zero.
     */
    private fun reset(calendar: Calendar, fields: List<Int>) {
        for (field in fields) {
            calendar.set(field, if (field == Calendar.DAY_OF_MONTH) 1 else 0)
        }
    }

    /**
     * Replace the values in the comma-separated list (case insensitive)
     * with their index in the list.
     * @return a new String with the values from the list replaced
     */
    private fun replaceOrdinals(value: String, commaSeparatedList: String): String {
        var value = value
        val list = commaSeparatedList.split(",")
        for (i in list.indices) {
            val item = list[i].toUpperCase()
            value = value.toUpperCase().replace(item, "$i")
        }
        return value
    }

    private fun setDaysOfMonth(bits: BitSet, field: String) {
        val max = 31
        // Days of month start with 1 (in Cron and Calendar) so add one
        setDays(bits, field, max + 1)
        // ... and remove it from the front
        bits.clear(0)
    }

    private fun setDays(bits: BitSet, field: String, max: Int) {
        var field = field
        if (field.contains("?")) {
            field = "*"
        }
        setNumberHits(bits, field, 0, max)
    }

    private fun setMonths(bits: BitSet, value: String) {
        var value = value
        val max = 12
        value = replaceOrdinals(value, "FOO,JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC")
        val months = BitSet(13)
        // Months start with 1 in Cron and 0 in Calendar, so push the values first into a longer bit set
        setNumberHits(months, value, 1, max + 1)
        // ... and then rotate it to the front of the months
        (1..max)
                .filter { months[it] }
                .forEach { bits.set(it - 1) }
    }

    private fun setNumberHits(bits: BitSet, value: String, min: Int, max: Int) {
        val fields = value.split(',')
        for (field in fields) {
            if (!field.contains("/")) {
                // Not an incrementer so it must be a range (possibly empty)
                val range = getRange(field, min, max)
                bits.set(range[0], range[1] + 1)
            } else {
                val split = field.split('/')
                if (split.size > 2) {
                    throw IllegalArgumentException("Incrementer has more than two fields: '$field' in expression \"$expression\"")
                }
                val range = getRange(split[0], min, max)
                if (!split[0].contains("-")) {
                    range[1] = max - 1
                }
                val delta = Integer.valueOf(split[1])!!
                if (delta <= 0) {
                    throw IllegalArgumentException("Incrementer delta must be 1 or higher: '$field' in expression \"$expression\"")
                }
                var i = range[0]
                while (i <= range[1]) {
                    bits.set(i)
                    i += delta
                }
            }
        }
    }

    private fun getRange(field: String, min: Int, max: Int): IntArray {
        val result = IntArray(2)
        if (field.contains("*")) {
            result[0] = min
            result[1] = max - 1
            return result
        }
        if (!field.contains("-")) {
            result[1] = Integer.valueOf(field)!!
            result[0] = result[1]
        } else {
            val split = field.split('-')
            if (split.size > 2) {
                throw IllegalArgumentException("Range has more than two fields: '$field' in expression \"$expression\"")
            }
            result[0] = Integer.valueOf(split[0])!!
            result[1] = Integer.valueOf(split[1])!!
        }
        if (result[0] >= max || result[1] >= max) {
            throw IllegalArgumentException("Range exceeds maximum ($max): '$field' in expression \"$expression\"")
        }
        if (result[0] < min || result[1] < min) {
            throw IllegalArgumentException("Range less than minimum ($min): '$field' in expression \"$expression\"")
        }
        if (result[0] > result[1]) {
            throw IllegalArgumentException("Invalid inverted range: '$field' in expression \"$expression\"")
        }
        return result
    }

    companion object {
        /**
         * Determine whether the specified expression represents a valid schedule pattern.
         *
         * Specifically, this method verifies that the expression contains six
         * fields separated by single spaces.
         * @param expression the expression to evaluate
         * @return `true` if the given expression is a valid schedule expression
         * @since 4.3
         */
        fun isValidExpression(expression: String): Boolean {
            return areValidCronFields(expression.split(' ')
                    .map(String::trim)
                    .filterNot(String::isEmpty))
        }

        private fun areValidCronFields(fields: Iterable<String>): Boolean = fields.toList().size == 6
    }
}

