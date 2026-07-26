package io.tekniq.crypto

import java.security.Key
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.SignatureException
import java.security.interfaces.RSAKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

private const val rsaDefaultBits = 2048
private const val dataKeyBits = 256
private const val ed25519RawSize = 32
private const val rsaOaepCipher = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

// The cipher name above only pins the message digest; SunJCE silently leaves MGF1 on SHA-1. Naming
// SHA-256 for both keeps ciphertext readable by providers and languages that don't share that quirk.
private val rsaOaepParams =
    OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)

// Ed25519 SubjectPublicKeyInfo header, immediately followed by the 32 raw key bytes.
private val ed25519Spki =
    byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)

private fun Key.base64() = Base64.getEncoder().encodeToString(encoded)

private fun rsaCipher(mode: Int, key: Key) = Cipher.getInstance(rsaOaepCipher)
    .apply { init(mode, key, rsaOaepParams) }

internal enum class TqKeyAlgorithm(val jca: String, val signature: String) {
    RSA("RSA", "SHA256withRSA"),
    ED25519("Ed25519", "Ed25519"),
}

/**
 * An asymmetric key pair. Each half holds its standard DER encoding as a Base64 string — the same
 * bytes a `BEGIN PRIVATE KEY` / `BEGIN PUBLIC KEY` PEM file carries — so a pair survives any string
 * store and round-trips through its own constructors.
 *
 * Signing and verification are available on every algorithm. Encryption is declared only on [Rsa]
 * because Ed25519 has no encryption scheme, so misuse is a compile error rather than a runtime one.
 */
sealed interface TqKeyPair {
    val privateKey: TqPrivateKey
    val publicKey: TqPublicKey

    fun sign(message: ByteArray): ByteArray = privateKey.sign(message)
    fun sign(message: String): String = privateKey.sign(message)
    fun verify(message: ByteArray, signature: ByteArray) = publicKey.verify(message, signature)
    fun verify(message: String, signature: String) = publicKey.verify(message, signature)

    data class Rsa(
        override val privateKey: TqPrivateKey.Rsa,
        override val publicKey: TqPublicKey.Rsa,
    ) : TqKeyPair {
        fun encrypt(message: ByteArray) = publicKey.encrypt(message)
        fun encrypt(message: String) = publicKey.encrypt(message)
        fun decrypt(ciphertext: ByteArray) = privateKey.decrypt(ciphertext)
        fun decrypt(ciphertext: String) = privateKey.decrypt(ciphertext)

        companion object {
            fun generate(bits: Int = rsaDefaultBits): Rsa = KeyPairGenerator
                .getInstance(TqKeyAlgorithm.RSA.jca)
                .apply { initialize(bits) }
                .generateKeyPair()
                .let { Rsa(TqPrivateKey.Rsa(it.private.base64()), TqPublicKey.Rsa(it.public.base64())) }
        }
    }

    data class Ed25519(
        override val privateKey: TqPrivateKey.Ed25519,
        override val publicKey: TqPublicKey.Ed25519,
    ) : TqKeyPair {
        companion object {
            fun generate(): Ed25519 = KeyPairGenerator
                .getInstance(TqKeyAlgorithm.ED25519.jca)
                .generateKeyPair()
                .let { Ed25519(TqPrivateKey.Ed25519(it.private.base64()), TqPublicKey.Ed25519(it.public.base64())) }
        }
    }
}

/** The private half of a [TqKeyPair], held as its Base64 PKCS#8 encoding. */
sealed class TqPrivateKey(internal val algorithm: TqKeyAlgorithm) {
    abstract val pkcs8: String

    @delegate:Transient
    protected val jca: java.security.PrivateKey by lazy {
        KeyFactory.getInstance(algorithm.jca)
            .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pkcs8)))
    }

    fun sign(message: ByteArray): ByteArray = Signature.getInstance(algorithm.signature).run {
        initSign(jca)
        update(message)
        sign()
    }

    fun sign(message: String): String = Base64.getEncoder().encodeToString(sign(message.toByteArray()))

    // Redacted, and final so the data subclasses can't regenerate a leaking toString().
    final override fun toString() = "${javaClass.simpleName}(pkcs8=REDACTED)"

    data class Rsa(override val pkcs8: String) : TqPrivateKey(TqKeyAlgorithm.RSA) {
        fun decrypt(ciphertext: ByteArray): ByteArray {
            val dataKey = rsaCipher(Cipher.DECRYPT_MODE, jca).doFinal(ciphertext, 0, wrappedKeySize)
            return TqCryptography.aesGcmDecrypt(ciphertext.copyOfRange(wrappedKeySize, ciphertext.size), dataKey)
        }

        fun decrypt(ciphertext: String) = String(decrypt(Base64.getDecoder().decode(ciphertext)))

        // The wrapped data key always fills exactly one RSA block, so where it ends is derivable
        // from the key itself and never has to be written into the ciphertext.
        private val wrappedKeySize: Int get() = ((jca as RSAKey).modulus.bitLength() + 7) / 8
    }

    data class Ed25519(override val pkcs8: String) : TqPrivateKey(TqKeyAlgorithm.ED25519)
}

/** The public half of a [TqKeyPair], held as its Base64 X.509 SubjectPublicKeyInfo encoding. */
sealed class TqPublicKey(internal val algorithm: TqKeyAlgorithm) {
    abstract val x509: String

    @delegate:Transient
    protected val jca: java.security.PublicKey by lazy {
        KeyFactory.getInstance(algorithm.jca)
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(x509)))
    }

    fun verify(message: ByteArray, signature: ByteArray): Boolean {
        val verifier = Signature.getInstance(algorithm.signature)
        verifier.initVerify(jca)
        verifier.update(message)
        return try {
            verifier.verify(signature)
        } catch (_: SignatureException) {
            false // structurally broken bytes are not a valid signature, so fail closed
        }
    }

    fun verify(message: String, signature: String): Boolean {
        val decoded = try {
            Base64.getDecoder().decode(signature)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return verify(message.toByteArray(), decoded)
    }

    data class Rsa(override val x509: String) : TqPublicKey(TqKeyAlgorithm.RSA) {
        /**
         * Encrypts any length of [message] by wrapping a single-use AES-GCM data key with RSA-OAEP.
         * RSA alone caps out below one key length, and chunking it would forfeit authentication.
         */
        fun encrypt(message: ByteArray): ByteArray {
            val dataKey = KeyGenerator.getInstance("AES").apply { init(dataKeyBits) }.generateKey().encoded
            return rsaCipher(Cipher.ENCRYPT_MODE, jca).doFinal(dataKey) +
                TqCryptography.aesGcmEncrypt(message, dataKey)
        }

        fun encrypt(message: String) = Base64.getEncoder().encodeToString(encrypt(message.toByteArray()))
    }

    data class Ed25519(override val x509: String) : TqPublicKey(TqKeyAlgorithm.ED25519) {
        /** The bare 32 bytes, as published by SSH, JWK and most non-JVM Ed25519 tooling. */
        fun raw(): ByteArray = Base64.getDecoder().decode(x509)
            .let { it.copyOfRange(it.size - ed25519RawSize, it.size) }

        companion object {
            fun ofRaw(raw: ByteArray): Ed25519 {
                require(raw.size == ed25519RawSize) { "Ed25519 public keys are $ed25519RawSize bytes" }
                return Ed25519(Base64.getEncoder().encodeToString(ed25519Spki + raw))
            }
        }
    }
}
