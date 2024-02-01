package io.tekniq.crypto

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.tekniq.crypto.TqCryptography.decrypt
import io.tekniq.crypto.TqCryptography.encrypt
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

    describe("Hmac SHA256 Digest") {
        it("correctly matches the hash") {
            val answer = "1c07f8a797b99a72a39b4542dc076cb810506914c1f7aeb00231cadf63129824"
            val msg = "Simple Digest Message"
            val digest = TqCryptography.hmac(msg, "Guardians of the Galaxy")
            digest shouldBe answer
        }
    }
})
