package io.tekniq.crypto

import java.io.*
import java.security.*
import java.security.spec.KeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TqCryptography {
    enum class Encoding { Base64, Hex }

    private const val bufferSize = 4096
    private const val blockSizeDenominator = 8
    private const val blockSizeDifference = 11

    infix fun TqKeyPair.decrypt(message: ByteArray): ByteArray = decrypt(message, this.privateKey)
    infix fun TqKeyPair.decrypt(message: String): String = decrypt(message, this.privateKey)
    infix fun TqKeyPair.encrypt(message: ByteArray): ByteArray = encrypt(message, this.publicKey)
    infix fun TqKeyPair.encrypt(message: String): String = encrypt(message, this.publicKey)
    infix fun TqKeyPair.sign(message: ByteArray): ByteArray = sign(message, this.privateKey)
    infix fun TqKeyPair.sign(message: String): String = sign(message, this.privateKey)
    infix fun TqKeyPair.verify(message: ByteArray): ByteArray = verify(message, this.publicKey)
    infix fun TqKeyPair.verify(message: String): String = verify(message, this.publicKey)

    fun decrypt(message: ByteArray, key: TqKeyPair.PrivateKey): ByteArray =
        transform(message, Cipher.DECRYPT_MODE, key.rsaKey, "RSA/ECB/OAEPWithMD5AndMGF1Padding")

    fun decrypt(message: String, key: TqKeyPair.PrivateKey): String =
        transform(message, Cipher.DECRYPT_MODE, key.rsaKey, "RSA/ECB/OAEPWithMD5AndMGF1Padding")

    fun encrypt(message: ByteArray, key: TqKeyPair.PublicKey): ByteArray =
        transform(message, Cipher.ENCRYPT_MODE, key.rsaKey, "RSA/ECB/OAEPWithMD5AndMGF1Padding")

    fun encrypt(message: String, key: TqKeyPair.PublicKey): String =
        transform(message, Cipher.ENCRYPT_MODE, key.rsaKey, "RSA/ECB/OAEPWithMD5AndMGF1Padding")

    fun sign(message: ByteArray, key: TqKeyPair.PrivateKey): ByteArray =
        transform(message, Cipher.ENCRYPT_MODE, key.rsaKey, "RSA")

    fun sign(message: String, key: TqKeyPair.PrivateKey): String =
        transform(message, Cipher.ENCRYPT_MODE, key.rsaKey, "RSA")

    fun verify(message: ByteArray, key: TqKeyPair.PublicKey): ByteArray =
        transform(message, Cipher.DECRYPT_MODE, key.rsaKey, "RSA")

    fun verify(message: String, key: TqKeyPair.PublicKey): String =
        transform(message, Cipher.DECRYPT_MODE, key.rsaKey, "RSA")

    fun generateKeyPair(bits: Int = 2048): TqKeyPair {
        val factory = KeyFactory.getInstance("RSA")
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(bits)
        val kp = gen.generateKeyPair()

        val privateKey = factory.getKeySpec(kp.private, RSAPrivateKeySpec::class.java)
        val publicKey = factory.getKeySpec(kp.public, RSAPublicKeySpec::class.java)
        return TqKeyPair(
            TqKeyPair.PrivateKey(privateKey.modulus, privateKey.privateExponent),
            TqKeyPair.PublicKey(publicKey.modulus, publicKey.publicExponent)
        )
    }

    fun b64Decode(text: ByteArray): ByteArray = Base64.getDecoder().decode(text)
    fun b64Decode(text: String): String = String(Base64.getDecoder().decode(text.toByteArray()))
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

    fun hmac(msg: ByteArray, key: ByteArray, algo: String = "HmacSHA256"): String {
        val mac = Mac.getInstance(algo)
            .also { it.init(SecretKeySpec(key, algo)) }
        val formatter = Formatter()
        return mac.doFinal(msg)
            .onEach { formatter.format("%02x", it) }
            .let { formatter.toString() }
    }

    private fun digest(text: ByteArray, algo: String, encoding: Encoding = Encoding.Hex): ByteArray {
        try {
            val digest = MessageDigest.getInstance(algo)
            val hash = digest.digest(text)
            return if (encoding == Encoding.Base64) b64Encode(hash) else return toHexString(hash).toByteArray()
        } catch (e: GeneralSecurityException) {
            error("Unable to hash contents: ${e.message}")
        }
    }

    private fun decode(input: InputStream, output: OutputStream, cipher: Cipher) {
        val message = StringBuilder()
        val buffer = ByteArray(bufferSize)
        while (true) { // TODO: What is the right/better way to do this in kotlin?
            val read = input.read(buffer)
            if (read <= 0) {
                break
            }

            message.append(String(buffer, 0, read))

            while (true) { // TODO: See outer loops question for improvement
                val indexOf = message.indexOf(" ")
                if (indexOf <= 0) {
                    break
                }

                val block = message.substring(0, indexOf).toByteArray()
                message.replace(0, indexOf + 1, "") // wipe out the processed portion and leave the rest
                decodeHelper(block, output, cipher)
            }
        }
        if (message.isNotEmpty()) {
            decodeHelper(message.toString().toByteArray(), output, cipher)
        }
    }

    private fun decodeHelper(block: ByteArray, out: OutputStream, cipher: Cipher) {
        val decode = Base64.getDecoder().decode(block)
        try {
            out.write(cipher.doFinal(decode))
        } catch (expected: Exception) {
            error("Error decrypting data: ${expected.message}")
        }
    }

    private fun encode(input: InputStream, output: OutputStream, cipher: Cipher, blockSize: Int) {
        var prepend = false
        val buffer = ByteArray(blockSize)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) {
                break
            }
            if (prepend) {
                output.write(" ".toByteArray())
            } else {
                prepend = true
            }

            try {
                val encrypted = cipher.doFinal(buffer, 0, read)
                val encode = Base64.getEncoder().encode(encrypted)
                output.write(encode)
            } catch (expected: Exception) {
                error("Error encrypting data: ${expected.message}")
            }
        }
    }

    private fun generateKeyFromSpec(spec: KeySpec): Key? = KeyFactory
        .getInstance("RSA")
        .let { factory ->
            when (spec) {
                is RSAPublicKeySpec -> return factory.generatePublic(spec)
                is RSAPrivateKeySpec -> return factory.generatePrivate(spec)
                else -> return null
            }
        }

    private fun getBlockSize(spec: KeySpec): Int {
        if (spec is RSAPublicKeySpec) {
            return spec.modulus.bitLength() / blockSizeDenominator - blockSizeDifference
        }
        if (spec is RSAPrivateKeySpec) {
            return spec.modulus.bitLength() / blockSizeDenominator - blockSizeDifference
        }
        throw UnsupportedOperationException("Only RSA Key Specs are supported!")
    }

    private fun toHexString(ba: ByteArray): String = ba.joinToString("", transform = { "%02x".format(it) })

    private fun transform(message: String, mode: Int, key: KeySpec, cipher: String): String =
        String(transform(message.toByteArray(), mode, key, cipher))

    private fun transform(message: ByteArray, mode: Int, key: KeySpec, cipherAlgorithm: String): ByteArray {
        try {
            val cipher = Cipher.getInstance(cipherAlgorithm)
            cipher.init(mode, generateKeyFromSpec(key))
            val bais = ByteArrayInputStream(message)
            val baos = ByteArrayOutputStream()
            if (Cipher.DECRYPT_MODE == mode) {
                decode(bais, baos, cipher)
            } else {
                encode(bais, baos, cipher, getBlockSize(key))
            }
            return baos.toByteArray()
        } catch (e: IOException) {
            error(e)
        } catch (e: GeneralSecurityException) {
            error(e)
        }
    }
}

