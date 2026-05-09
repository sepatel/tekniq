package io.tekniq.crypto

import java.security.MessageDigest
import java.util.Base64

@Deprecated("MD5 is cryptographically broken. Use io.tekniq.crypto.TqCryptography.sha256 instead.", level = DeprecationLevel.WARNING)
object UnsafeHash {
    enum class Encoding { Base64, Hex }

    fun md5(text: ByteArray, encoding: Encoding = Encoding.Hex): ByteArray = digest(text, "MD5", encoding)

    fun md5(text: String, encoding: Encoding = Encoding.Hex): String =
        String(digest(text.toByteArray(), "MD5", encoding))

    private fun digest(text: ByteArray, algo: String, encoding: Encoding = Encoding.Hex): ByteArray {
        val digest = MessageDigest.getInstance(algo)
        val hash = digest.digest(text)
        return when (encoding) {
            Encoding.Base64 -> Base64.getEncoder().encode(hash)
            else -> hash.joinToString("", transform = { "%02x".format(it) }).toByteArray()
        }
    }
}