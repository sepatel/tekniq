package io.tekniq.validation

open class CheckException(val reasons: Collection<DeniedReason>) : Exception() {
    override val message: String?
        get() = reasons.joinToString { it.message }
}
