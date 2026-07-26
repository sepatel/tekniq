package io.tekniq.crypto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

object TqCryptoSpec : DescribeSpec({
    describe("Unified key pair behaviour") {
        // The whole point of the sealed hierarchy: algorithm-agnostic code signs and verifies without
        // knowing or caring which algorithm backs the pair.
        val pairs = listOf<TqKeyPair>(TqKeyPair.Rsa.generate(), TqKeyPair.Ed25519.generate())

        pairs.forEach { keyPair ->
            val name = keyPair::class.simpleName

            it("signs and verifies bytes for $name") {
                val message = "Guardians of the Galaxy".toByteArray()
                keyPair.verify(message, keyPair.sign(message)) shouldBe true
            }

            it("rejects a tampered message for $name") {
                val signature = keyPair.sign("the original message")
                keyPair.verify("a tampered message", signature) shouldBe false
            }

            it("fails closed on malformed signatures for $name") {
                keyPair.verify("message".toByteArray(), ByteArray(12)) shouldBe false
                keyPair.verify("message", "not valid base64 !!") shouldBe false
            }

            it("keeps private key material out of toString for $name") {
                keyPair.privateKey.toString() shouldNotContain keyPair.privateKey.pkcs8
                keyPair.toString() shouldNotContain keyPair.privateKey.pkcs8
            }
        }
    }

    describe("RSA hybrid encryption") {
        val keyPair = TqKeyPair.Rsa.generate()

        it("round trips a string") {
            val message = "Hello Flying Purple Monkey!"
            keyPair.decrypt(keyPair.encrypt(message)) shouldBe message
        }

        // The previous chunking implementation silently broke past ~222 bytes for a 2048 bit key.
        it("round trips a payload far larger than the modulus") {
            val message = "flying purple monkey ".repeat(5_000).toByteArray()
            keyPair.decrypt(keyPair.encrypt(message)) shouldBe message
        }

        it("round trips an empty payload") {
            keyPair.decrypt(keyPair.encrypt(ByteArray(0))) shouldBe ByteArray(0)
        }

        it("produces a different ciphertext every time") {
            val message = "repeatable plaintext"
            keyPair.encrypt(message) shouldNotBe keyPair.encrypt(message)
        }

        it("detects a tampered ciphertext") {
            val ciphertext = keyPair.encrypt("authenticated payload".toByteArray())
            ciphertext[ciphertext.size - 1] = (ciphertext.last() + 1).toByte()
            shouldThrowAny { keyPair.decrypt(ciphertext) }
        }

        it("cannot be decrypted by an unrelated key") {
            val ciphertext = keyPair.encrypt("secret".toByteArray())
            shouldThrowAny { TqKeyPair.Rsa.generate().decrypt(ciphertext) }
        }

        it("scales to a larger modulus") {
            val large = TqKeyPair.Rsa.generate(bits = 3072)
            large.decrypt(large.encrypt("sized to the key")) shouldBe "sized to the key"
        }
    }

    describe("Key persistence") {
        it("reconstructs an RSA pair from its encodings") {
            val original = TqKeyPair.Rsa.generate()
            val restored = TqKeyPair.Rsa(
                TqPrivateKey.Rsa(original.privateKey.pkcs8),
                TqPublicKey.Rsa(original.publicKey.x509),
            )

            restored shouldBe original
            original.verify("crossed", restored.sign("crossed")) shouldBe true
            restored.decrypt(original.encrypt("crossed")) shouldBe "crossed"
        }

        it("reconstructs an Ed25519 pair from its encodings") {
            val original = TqKeyPair.Ed25519.generate()
            val restored = TqKeyPair.Ed25519(
                TqPrivateKey.Ed25519(original.privateKey.pkcs8),
                TqPublicKey.Ed25519(original.publicKey.x509),
            )

            restored shouldBe original
            original.verify("crossed", restored.sign("crossed")) shouldBe true
        }

        it("verifies with only the public half") {
            val keyPair = TqKeyPair.Ed25519.generate()
            val signature = keyPair.sign("detached".toByteArray())

            TqPublicKey.Ed25519(keyPair.publicKey.x509).verify("detached".toByteArray(), signature) shouldBe true
        }
    }

    describe("Ed25519 specifics") {
        val keyPair = TqKeyPair.Ed25519.generate()

        it("produces 64 byte signatures") {
            keyPair.sign("anything".toByteArray()) shouldHaveSize 64
        }

        it("round trips a raw 32 byte public key") {
            val raw = keyPair.publicKey.raw()
            raw shouldHaveSize 32
            TqPublicKey.Ed25519.ofRaw(raw) shouldBe keyPair.publicKey

            val message = "raw public key".toByteArray()
            TqPublicKey.Ed25519.ofRaw(raw).verify(message, keyPair.sign(message)) shouldBe true
        }

        it("rejects a raw key of the wrong length") {
            shouldThrow<IllegalArgumentException> { TqPublicKey.Ed25519.ofRaw(ByteArray(31)) }
        }

        // RFC 8032 section 7.1, TEST 1: the canonical empty-message vector.
        it("verifies the RFC 8032 empty message vector") {
            val publicKey = TqPublicKey.Ed25519.ofRaw(
                "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a".hexBytes()
            )
            val signature = (
                "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
                    "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"
                ).hexBytes()

            publicKey.verify(ByteArray(0), signature) shouldBe true
            publicKey.verify("not the empty message".toByteArray(), signature) shouldBe false
        }
    }

    describe("AES-GCM") {
        val key = ByteArray(32) { it.toByte() }

        it("round trips a payload") {
            val plaintext = "symmetric secret".toByteArray()
            TqCryptography.aesGcmDecrypt(TqCryptography.aesGcmEncrypt(plaintext, key), key) shouldBe plaintext
        }

        it("detects tampering") {
            val ciphertext = TqCryptography.aesGcmEncrypt("authentic".toByteArray(), key)
            ciphertext[ciphertext.size - 1] = (ciphertext.last() + 1).toByte()
            shouldThrowAny { TqCryptography.aesGcmDecrypt(ciphertext, key) }
        }

        it("uses a fresh nonce per call") {
            val plaintext = "same input".toByteArray()
            TqCryptography.aesGcmEncrypt(plaintext, key) shouldNotBe TqCryptography.aesGcmEncrypt(plaintext, key)
        }
    }

    describe("Hashing Algorithms") {
        it("correctly hashes via sha-256") {
            TqCryptography.sha256("I am a flying purple monkey") shouldBe
                "36e590219098e573561b3cd3f703193f94f5d3f5e8f2cbc3f75468e06b6ba132"
        }
        it("correctly hashes via md5") {
            TqCryptography.md5("I am a flying purple monkey") shouldBe "e1a3401853a457a79917b7a59e975333"
        }
        it("encodes a digest as base64 on request") {
            TqCryptography.sha256("I am a flying purple monkey", TqCryptography.Encoding.Base64) shouldBe
                "NuWQIZCY5XNWGzzT9wMZP5T10/Xo8svD91Ro4GtroTI="
        }
    }

    describe("Hmac SHA256 Digest") {
        it("correctly matches the hash") {
            TqCryptography.hmac("Simple Digest Message", "Guardians of the Galaxy") shouldBe
                "1c07f8a797b99a72a39b4542dc076cb810506914c1f7aeb00231cadf63129824"
        }
    }

    describe("Base64") {
        it("round trips a string") {
            TqCryptography.b64Decode(TqCryptography.b64Encode("flying purple monkey")) shouldBe
                "flying purple monkey"
        }
    }
})

private fun String.hexBytes() = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
