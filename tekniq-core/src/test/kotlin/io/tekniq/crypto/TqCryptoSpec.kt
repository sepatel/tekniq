package io.tekniq.crypto

import io.kotest.core.spec.style.DescribeSpec
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
})
