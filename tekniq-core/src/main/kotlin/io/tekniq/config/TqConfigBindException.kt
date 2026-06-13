package io.tekniq.config

class TqConfigBindException(
    message: String,
    val path: String? = null,
    val expectedType: String? = null,
    val actualValue: Any? = null,
    cause: Throwable? = null
) : RuntimeException(formatMessage(message, path, expectedType, actualValue), cause) {
    companion object {
        private fun formatMessage(
            message: String,
            path: String?,
            expectedType: String?,
            actualValue: Any?
        ): String {
            val parts = mutableListOf(message)
            if (path != null) parts += "at '$path'"
            if (expectedType != null) parts += "expected $expectedType"
            if (actualValue != null) {
                val display = if (actualValue.toString().length > 64) {
                    actualValue.toString().substring(0, 64) + "..."
                } else {
                    actualValue.toString()
                }
                parts += "actual=$display"
            } else if (actualValue === NOTHING) {
                parts += "actual=null"
            }
            return parts.joinToString("; ")
        }

        private val NOTHING = Any()
    }
}
