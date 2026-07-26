package io.tekniq.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Keyless cryptographic primitives: symmetric AES-GCM, digests, HMAC and Base64. Asymmetric keys
 * and their operations live on [TqKeyPair], [TqPrivateKey] and [TqPublicKey].
 */
object TqCryptography {
    enum class Encoding { Base64, Hex }

    private const val gcmCipher = "AES/GCM/NoPadding"
    private const val gcmNonceSize = 12
    private const val gcmTagBits = 128
    private val random = SecureRandom()

    /** Returns `nonce || ciphertext || tag`; the nonce is fresh per call and never reused. */
    fun aesGcmEncrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        val nonce = ByteArray(gcmNonceSize).also(random::nextBytes)
        val cipher = Cipher.getInstance(gcmCipher)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(gcmTagBits, nonce))
        return nonce + cipher.doFinal(plaintext)
    }

    /** Throws [javax.crypto.AEADBadTagException] if the ciphertext or its nonce was altered. */
    fun aesGcmDecrypt(ciphertext: ByteArray, key: ByteArray): ByteArray {
        val nonce = ciphertext.copyOf(gcmNonceSize)
        val cipher = Cipher.getInstance(gcmCipher)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(gcmTagBits, nonce))
        return cipher.doFinal(ciphertext, gcmNonceSize, ciphertext.size - gcmNonceSize)
    }

    fun b64Decode(text: ByteArray): ByteArray = Base64.getDecoder().decode(text)
    fun b64Decode(text: String): String = String(Base64.getDecoder().decode(text))
    fun b64Encode(text: ByteArray): ByteArray = Base64.getEncoder().encode(text)
    fun b64Encode(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray())

    fun sha256(text: ByteArray, encoding: Encoding = Encoding.Hex): ByteArray = digest(text, "SHA-256", encoding)
    fun sha256(text: String, encoding: Encoding = Encoding.Hex): String =
        String(digest(text.toByteArray(), "SHA-256", encoding))

    fun md5(text: ByteArray, encoding: Encoding = Encoding.Hex): ByteArray = digest(text, "MD5", encoding)
    fun md5(text: String, encoding: Encoding = Encoding.Hex): String =
        String(digest(text.toByteArray(), "MD5", encoding))

    fun hmac(msg: String, key: String, algo: String = "HmacSHA256"): String =
        hmac(msg.toByteArray(), key.toByteArray(), algo)

    fun hmac(msg: ByteArray, key: ByteArray, algo: String = "HmacSHA256"): String = Mac.getInstance(algo)
        .apply { init(SecretKeySpec(key, algo)) }
        .doFinal(msg)
        .let(::toHexString)

    private fun digest(text: ByteArray, algo: String, encoding: Encoding): ByteArray =
        MessageDigest.getInstance(algo).digest(text).let {
            if (encoding == Encoding.Base64) b64Encode(it) else toHexString(it).toByteArray()
        }

    private fun toHexString(ba: ByteArray) = ba.joinToString("") { "%02x".format(it) }
}
