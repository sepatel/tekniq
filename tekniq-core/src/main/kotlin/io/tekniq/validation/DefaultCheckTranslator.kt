package io.tekniq.validation

object DefaultCheckTranslator : TqCheckTranslator {
    override fun translate(code: String, field: String?, at: String?, message: String?, data: Any?): DeniedReason {
        val reasonCode = listOfNotNull(code, field, at?.let { "at $it" }).joinToString(" ")
        return DefaultDeniedReason(reasonCode, convertMessage(message ?: reasonCode, data))
    }

    private fun convertMessage(msg: String, data: Any?): String = data?.let { src ->
        msg.replace("""\{\{(.+?)}}""".toRegex()) { match ->
            src.let { TqCheck.getValue(it, match.groupValues[1]).toString() }
        }
    } ?: msg
}
