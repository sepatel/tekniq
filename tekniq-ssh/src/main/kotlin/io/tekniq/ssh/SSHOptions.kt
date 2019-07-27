package io.tekniq.ssh

import com.jcraft.jsch.*
import java.io.File.separator as FS

data class SSHIdentity(
        val privkey: String,
        val passphrase: String? = null
)

/**
 * io.tekniq.ssh.SSHOptions stores all ssh parameters
 */
data class SSHOptions(
        val host: String = "localhost",
        val username: String = "",
        val password: String? = null,
        val passphrase: String? = null,
        val name: String? = null,
        val port: Int = 22,
        val prompt: String? = null,
        val timeout: Long = 0,
        val connectTimeout: Long = 30000,
        val retryCount: Int = 5,
        val retryDelay: Int = 2000,
        val identities: List<SSHIdentity> = SSHIdentities.defaultIdentities,
        val charset: String = "ISO-8859-15",
        val noneCipher: Boolean = false,
        val compress: Int? = null,
        val execWithPty: Boolean = false,
        val ciphers: List<String> = "aes128-ctr,aes128-cbc,3des-ctr,3des-cbc,blowfish-cbc,aes192-ctr,aes192-cbc,aes256-ctr,aes256-cbc".split(","),
        val proxy: Proxy? = null,
        val sessionConfig: Map<String, String> = mutableMapOf(),
        val openSSHConfig: String? = null,
        val knownHostsFile: String? = null
) {
    fun compressed(): SSHOptions = this.copy(compress = 5)
    fun viaProxyHttp(host: String, port: Int = 80): SSHOptions = this.copy(proxy = ProxyHTTP(host, port))
    fun viaProxySOCKS4(host: String, port: Int = 1080): SSHOptions = this.copy(proxy = ProxySOCKS4(host, port))
    fun viaProxySOCKS5(host: String, port: Int = 1080): SSHOptions = this.copy(proxy = ProxySOCKS5(host, port))
    fun addIdentity(identity: SSHIdentity) = this.copy(identities = (identities + identity))
}

object SSHIdentities {
    val userHome: String = System.getProperty("user.home", "")
    val defaultPrivKeyFilenames = listOf(
            "identity",
            "id_dsa",
            "id_ecdsa",
            "id_ed25519",
            "id_rsa"
    )
    val defaultIdentities = defaultPrivKeyFilenames
            .map { userHome + FS + ".ssh" + FS + it }
            .map { SSHIdentity(it) }
}
