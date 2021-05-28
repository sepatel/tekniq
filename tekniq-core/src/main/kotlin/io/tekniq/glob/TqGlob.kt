package io.tekniq.glob

/**
 * Logic for implementation taken from https://github.com/fitzgen/glob-to-regexp/blob/master/index.js
 *
 * @author Nick Fitzgerald
 * @author Sejal Patel
 */
object TqGlob {
    fun toRegEx(
        pattern: String,
        insensitive: Boolean = false,    // pattern is case insensitive
        contains: Boolean = false,      // if true, pattern is subset in string
        globstar: Boolean = true,       // if false * will behave like **
        extended: Boolean = true,       // bashlike support for matching enhancements
    ): Regex {
        val regexPattern = StringBuilder()
        var inGroup = false
        var prev: Char? = null
        var next: Char? = null
        var starCount = 0
        pattern.forEachIndexed { index, it ->
            // completion of globstar pattern, skip over the /
            if (starCount == -1) {
                starCount = 0
                return@forEachIndexed
            }
            // state machine for globstar pattern detection
            if (it != '*') starCount = 0
            next = if (index + 1 < pattern.length) pattern[index + 1] else null
            when (it) {
                '/', '$', '^', '+', '.', '(', ')', '=', '!', '|' ->
                    regexPattern.append('\\').append(it)
                '?' -> if (extended) regexPattern.append('.') else regexPattern.append("\\?")
                '[', ']' -> if (extended) regexPattern.append(it) else regexPattern.append('\\').append(it)
                '{' -> if (extended) regexPattern.append('(').also { inGroup = true } else regexPattern.append("\\{")
                '}' -> if (extended) regexPattern.append(')').also { inGroup = false } else regexPattern.append("\\}")
                ',' -> {
                    if (inGroup) regexPattern.append('|')
                    else regexPattern.append('\\').append(it)
                }
                '*' -> {
                    starCount++
                    if (!globstar) regexPattern.append(".*")
                    else {
                        val match = starCount > 1
                                && (prev == null || prev == '/')
                                && (next == null || next == '/')
                        if (match) {
                            regexPattern.append("((?:[^/]*(?:\\/|\$))*)")
                            starCount = -1
                        } else {
                            regexPattern.append("([^/]*)")
                        }
                    }
                }
                else -> regexPattern.append(it)
            }
            if (it != '*') prev = it
        }

        if (!contains) regexPattern.insert(0, '^').append('$')
        else regexPattern.insert(0, ".*").append(".*")
        val regexFlags = setOfNotNull(
            if (insensitive) RegexOption.IGNORE_CASE else null
        )
        return regexPattern.toString().toRegex(regexFlags)
    }
}
