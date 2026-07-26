package io.tekniq.crypto

import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class TqEd25519KeyPair(val privateKey: PrivateKey, val publicKey: PublicKey) {
    data class PrivateKey(val pkcs8: String) {
        @delegate:Transient
        private val key: java.security.PrivateKey by lazy {
            KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pkcs8)))
        }

        internal fun decode(): java.security.PrivateKey = key

        override fun toString(): String = "PrivateKey(REDACTED)"
    }

    data class PublicKey(val x509: String) {
        @delegate:Transient
        private val key: java.security.PublicKey by lazy {
            KeyFactory.getInstance(ALGORITHM)
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(x509)))
        }

        internal fun decode(): java.security.PublicKey = key
    }

    companion object {
        internal const val ALGORITHM = "Ed25519"
    }
}
