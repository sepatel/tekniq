package io.tekniq.crypto

import java.math.BigInteger
import java.security.spec.KeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec

data class TqKeyPair(val privateKey: PrivateKey, val publicKey: PublicKey) {
    data class PrivateKey(val modulus: BigInteger, val privateExponent: BigInteger) : KeySpec {
        @delegate:Transient
        val rsaKey: RSAPrivateKeySpec by lazy { RSAPrivateKeySpec(modulus, privateExponent) }
    }

    data class PublicKey(val modulus: BigInteger, val publicExponent: BigInteger) : KeySpec {
        @delegate:Transient
        val rsaKey: RSAPublicKeySpec by lazy { RSAPublicKeySpec(modulus, publicExponent) }
    }
}
