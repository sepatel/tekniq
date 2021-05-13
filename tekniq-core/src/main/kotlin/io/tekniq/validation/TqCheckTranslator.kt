package io.tekniq.validation

// TODO: Explain that if i18n translator is desired, it can be translated here using the current Locale
interface TqCheckTranslator {
    /**
     * @param code short code for the denial (like Required or Invalid).
     * @param field extra level of precision for what the reason applies to (like path param or data point).
     * @param at extra level of precision for where the reason applies to (like page or area of code).
     * @param message message template.
     * @param data data to be applied to the message template
     */
    fun translate(
        code: String,
        field: String? = null,
        at: String? = null,
        message: String? = null,
        data: Any? = null
    ): DeniedReason
}
