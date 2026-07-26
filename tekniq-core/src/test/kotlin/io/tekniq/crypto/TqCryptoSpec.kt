package io.tekniq.crypto

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.tekniq.crypto.TqCryptography.decrypt
import io.tekniq.crypto.TqCryptography.encrypt
import io.tekniq.crypto.TqCryptography.sign
import io.tekniq.crypto.TqCryptography.verify
import java.util.Base64
import kotlin.test.assertEquals

object TqCryptoSpec : DescribeSpec({
    describe("Encryption and Decryption") {
        val keypair = TqCryptography.generateKeyPair()
        it("Encrypt a message") {
            val encrypted = keypair.encrypt("Hello Flying Purple Monkey!")
            val decrypted = keypair.decrypt(encrypted)
            assertEquals("Hello Flying Purple Monkey!", decrypted)
        }
    }

    describe("Hashing Algorithms") {
        it("correctly hashes via sha-256") {
            val hash = TqCryptography.sha256("I am a flying purple monkey")
            assertEquals("36e590219098e573561b3cd3f703193f94f5d3f5e8f2cbc3f75468e06b6ba132", hash)
        }
        it("correctly hashes via md5") {
            val hash = TqCryptography.md5("I am a flying purple monkey")
            assertEquals("e1a3401853a457a79917b7a59e975333", hash)
        }
    }

    describe("Ed25519 signatures") {
        val keyPair = TqCryptography.generateEd25519KeyPair()

        it("signs and verifies a detached signature") {
            val message = "Hello Flying Purple Monkey!".toByteArray()
            val signature = keyPair.sign(message)

            signature.size shouldBe 64
            keyPair.verify(message, signature) shouldBe true
            keyPair.verify("tampered".toByteArray(), signature) shouldBe false
        }

        it("reconstructs keys from their persisted encodings") {
            val restored = TqEd25519KeyPair(
                TqEd25519KeyPair.PrivateKey(keyPair.privateKey.pkcs8),
                TqEd25519KeyPair.PublicKey(keyPair.publicKey.x509),
            )
            val message = "persisted".toByteArray()

            restored.verify(message, restored.sign(message)) shouldBe true
            restored.toString().contains(keyPair.privateKey.pkcs8) shouldBe false
        }

        it("accepts a raw 32-byte public key") {
            val encoded = Base64.getDecoder().decode(keyPair.publicKey.x509)
            val raw = encoded.copyOfRange(encoded.size - 32, encoded.size)
            val publicKey = TqCryptography.ed25519PublicKey(raw)
            val message = "raw public key".toByteArray()

            TqCryptography.verify(message, keyPair.sign(message), publicKey) shouldBe true
        }

        it("fails closed for malformed input") {
            keyPair.verify("message".toByteArray(), ByteArray(12)) shouldBe false
            shouldThrow<IllegalArgumentException> {
                TqCryptography.ed25519PublicKey(ByteArray(31))
            }
        }

        it("verifies the RFC 8032 empty-message vector") {
            val publicKey = TqCryptography.ed25519PublicKey(
                "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a".hexBytes()
            )
            val signature = (
                "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
                    "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"
                ).hexBytes()

            TqCryptography.verify(byteArrayOf(), signature, publicKey) shouldBe true
        }
    }

    describe("Hmac SHA256 Digest") {
        it("correctly matches the hash") {
            val answer = "1c07f8a797b99a72a39b4542dc076cb810506914c1f7aeb00231cadf63129824"
            val msg = "Simple Digest Message"
            val digest = TqCryptography.hmac(msg, "Guardians of the Galaxy")
            digest shouldBe answer
        }
    }
})

private fun String.hexBytes(): ByteArray = ByteArray(length / 2) { index ->
    substring(index * 2, index * 2 + 2).toInt(16).toByte()
}
