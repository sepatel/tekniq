package io.tekniq.ssh

import com.jcraft.jsch.*
import io.tekniq.ssh.operations.AllOperations
import io.tekniq.ssh.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*

open class SSH(val options: SSHOptions) : AllOperations, Closeable {

    companion object {

        /**
         * Executes [withssh] in a new ssh session and then close the session
         */
        fun <T> once(
                host: String = "localhost",
                username: String = System.getProperty("user.name", ""),
                password: String? = null,
                passphrase: String? = null,
                port: Int = 22,
                timeout: Long = 300000L,
                withssh: SSH.() -> T
        ): T = once(SSHOptions(
                host = host, username = username, password = password, passphrase = passphrase, port = port, timeout = timeout
        ), withssh)

        /**
         * Executes [withssh] in a new ssh session and then close the session
         */
        fun <T> once(options: SSHOptions, withssh: SSH.() -> T): T = SSH(options).use { it.withssh() }

        /**
         * Executes [withssh] in a new ssh shell channel and then close the session
         */
        fun <T> shell(
                host: String = "localhost",
                username: String = System.getProperty("user.name", ""),
                password: String? = null,
                passphrase: String? = null,
                port: Int = 22,
                timeout: Long = 300000L,
                withssh: SSHShell.() -> T
        ): T = shell(SSHOptions(
                host = host, username = username, password = password, passphrase = passphrase, port = port, timeout = timeout
        ), withssh)

        /**
         * Executes [withssh] in a new ssh shell channel and then close the session
         */
        fun <T> shell(options: SSHOptions, withssh: SSHShell.() -> T): T = SSH(options).shell(withssh)

        /**
         * Executes [withssh] in a new ssh powershell channel and then close the session
         */
        fun <T> powershell(
                host: String = "localhost",
                username: String = System.getProperty("user.name", ""),
                password: String? = null,
                passphrase: String? = null,
                port: Int = 22,
                timeout: Long = 300000L,
                withssh: SSHPowerShell.() -> T
        ): T = powershell(SSHOptions(
                host = host, username = username, password = password, passphrase = passphrase, port = port, timeout = timeout
        ), withssh)

        /**
         * Executes [withssh] in a new ssh powershell channel and then close the session
         */
        fun <T> powershell(options: SSHOptions, withssh: SSHPowerShell.() -> T): T = SSH(options).powershell(withssh)

        /**
         * Executes [withssh] in a new ssh ftp channel and then close the session
         */
        fun <T> ftp(
                host: String = "localhost",
                username: String = System.getProperty("user.name", ""),
                password: String? = null,
                passphrase: String? = null,
                port: Int = 22,
                timeout: Long = 300000L,
                withssh: SSHFtp.() -> T
        ): T = ftp(SSHOptions(
                host = host, username = username, password = password, passphrase = passphrase, port = port, timeout = timeout
        ), withssh)

        /**
         * Executes [withssh] in a new ssh ftp channel and then close the session
         */
        fun <T> ftp(options: SSHOptions, withssh: SSHFtp.() -> T): T = SSH(options).ftp(withssh)
    }

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val jsch: JSch = JSch()
    private var firstHasFailed = false

    val noerr: (data: ExecResult) -> Any = {}

    lateinit var checkedsession: Session
    fun jschsession(): Session {
        if (!::checkedsession.isInitialized) {
            checkedsession = buildSession()
        }
        try {
            checkedsession.sendKeepAliveMsg()
            val testChannel = checkedsession.openChannel("exec") as ChannelExec
            testChannel.setCommand("true")
            testChannel.connect()
            testChannel.disconnect()
        } catch (ex: Exception) {
            try {
                checkedsession.disconnect()
            } catch (e: Exception) {
            }
            logger.warn("Session is KO, reconnecting...")
            checkedsession = buildSession()
        }
        return checkedsession

    }

    /**
     * Executes the given [cmd] and returns the result as a String
     */
    override fun execute(cmd: String): String = execOnce(cmd)

    /**
     * Executes the given [cmd] and returns the result as a String, with the exit code as a Pair
     */
    override fun executeWithStatus(cmd: String): Pair<String, Int> = execOnceWithStatus(cmd)

    /**
     * Returns possible content from a remote file called [remoteFilename] as a string
     */
    override fun get(remoteFilename: String): String? = opWithFallback(
            { scp { get(remoteFilename) } },
            { ftp { get(remoteFilename) } }
    )

    /**
     * Returns possible content from a remote file called [remoteFilename] as an array of bytes
     */
    override fun getBytes(remoteFilename: String): ByteArray? = opWithFallback(
            { scp { getBytes(remoteFilename) } },
            { ftp { getBytes(remoteFilename) } }
    )

    /**
     * Copies the remote file called [remoteFilename] to the local [outputStream]
     */
    override fun receive(remoteFilename: String, outputStream: OutputStream) = opWithFallback(
            { scp { receive(remoteFilename, outputStream) } },
            { ftp { receive(remoteFilename, outputStream) } }
    )

    /**
     * Uploads string [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun put(data: String, remoteDestination: String) = opWithFallback(
            { scp { put(data, remoteDestination) } },
            { ftp { put(data, remoteDestination) } }
    )

    /**
     * Uploads byte array [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun putBytes(data: ByteArray, remoteDestination: String) = opWithFallback(
            { scp { putBytes(data, remoteDestination) } },
            { ftp { putBytes(data, remoteDestination) } }
    )

    /**
     * Uploads [howmany] bytes of input stream [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun putFromStream(data: InputStream, howmany: Int, remoteDestination: String) = scp { putFromStream(data, howmany, remoteDestination) }

    /**
     * Copies a [localFile] to a remote file at [remoteDestination]
     */
    override fun send(localFile: File, remoteDestination: String) = opWithFallback(
            { scp { send(localFile, remoteDestination) } },
            { ftp { send(localFile, remoteDestination) } }
    )

    /**
     * Pipes [data] into a remote [filespec]
     */
    override fun catData(data: String, filespec: String): Boolean {
        put(data, filespec)
        return true //TODO
    }

    /**
     * Closes the JSch channel.
     */
    override fun close() {
        jschsession().disconnect()
    }

    /**
     * Executes [proc], providing an instance of an SSHShell
     */
    fun <T> shell(proc: SSHShell.() -> T): T = SSHShell(this).use { it.proc() }

    /**
     * Executes [proc], providing an instance of an SSHPowerShell
     */
    fun <T> powershell(proc: SSHPowerShell.() -> T): T = SSHPowerShell(this).use { it.proc() }

    /**
     * Executes [proc], providing an instance of an SSHFtp
     */
    fun <T> ftp(proc: SSHFtp.() -> T): T = SSHFtp(this).use { it.proc() }

    /**
     * Executes [proc], providing an instance of an SSHScp
     */
    fun <T> scp(proc: SSHScp.() -> T): T = SSHScp(this).use { it.proc() }

    /**
     * Runs [cmd] on the remote servers, sending stdout to [out] and stderr to [err]
     */
    fun run(cmd: String, out: ExecResult.() -> Unit, err: ExecResult.() -> Unit = {}) = SSHExec(cmd, out, err, this)

    /**
     * Opens an exec channel solely to execute the [scmd], returning the trimmed stdout.
     */
    fun execOnceAndTrim(scmd: String): String = execOnce(scmd).trim()

    /**
     * Opens an exec channel solely to execute the [scmd], returning stdout.
     */
    fun execOnce(scmd: String): String {
        val (result, _) = execOnceWithStatus(scmd)
        return result
    }

    /**
     * Opens an exec channel solely to execute the [scmd], returning stdout and the exit code.
     */
    fun execOnceWithStatus(scmd: String): Pair<String, Int> {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        var exitCode = -1

        val outputReceiver = fun(buffer: StringBuilder): ExecResult.() -> Unit = {
            when (this) {
                is ExecPart -> {
                    if (buffer.length > 0) buffer.append("\n")
                    buffer.append(content)
                }
                is ExecEnd -> exitCode = rc
                else -> Unit
            }
        }

        var runner: SSHExec? = null
        try {
            runner = SSHExec(scmd, outputReceiver(stdout), outputReceiver(stderr), this)
            runner.waitForEnd()
        } catch (e: InterruptedException) {
            throw SSHTimeoutException(stdout.toString(), stderr.toString())
        } finally {
            runner?.close()
        }

        return Pair(stdout.toString(), exitCode)
    }

    /**
     * Forwards port [hport] on remote [host] to local port [lport]
     */
    fun remoteToLocal(lport: Int, host: String, hport: Int): Int {
        return jschsession().setPortForwardingL(lport, host, hport)
    }

    /**
     * Forwards port [hport] on remote [host] to an automatically chosen local port
     */
    fun remoteToLocal(host: String, hport: Int): Int {
        return jschsession().setPortForwardingL(0, host, hport)
    }

    /**
     * Forwards port [lport] from local [lhost] ot remote port [rport]
     */
    fun localToRemote(rport: Int, lhost: String, lport: Int) {
        jschsession().setPortForwardingR(rport, lhost, lport)
    }

    /**
     * Connects to a remote SSH through this SSH session using [options]
     */
    fun remote(remoteOptions: SSHOptions): SSH {
        val chosenPort = remoteToLocal(remoteOptions.host, remoteOptions.port)
        val localOptions = remoteOptions.copy(host = "127.0.0.1", port = chosenPort)
        return SSH(localOptions)
    }

    /**
     * Connects to a remote SSH through this SSH session
     */
    fun remote(
            host: String = "localhost",
            username: String = System.getProperty("user.name", ""),
            password: String? = null,
            passphrase: String? = null,
            port: Int = 22,
            timeout: Long = 300000L
    ): SSH = remote(SSHOptions(
            host = host, username = username, password = password, passphrase = passphrase, port = port, timeout = timeout
    ))

    /**
     * Provides a new shell for the current SSH session. Must be closed manually.
     */
    fun newShell(): SSHShell = SSHShell(this)

    /**
     * Provides a new powershell for the current SSH session. Must be closed manually.
     */
    fun newPowerShell(): SSHPowerShell = SSHPowerShell(this)

    /**
     * Provides a new ftp for the current SSH session. Must be closed manually.
     */
    fun newSftp(): SSHFtp = SSHFtp(this)

    private fun buildSession(): Session {
        options.identities.forEach { ident ->
            val fident = File(ident.privkey)
            if (fident.isFile) {
                val pass = ident.passphrase ?: options.passphrase
                jsch.addIdentity(fident.absolutePath, pass)
            }
        }

        if (options.openSSHConfig != null) {
            jsch.configRepository = OpenSSHConfig.parseFile(options.openSSHConfig)
        }

        if (options.knownHostsFile != null) {
            jsch.setKnownHosts(options.knownHostsFile)
        }

        val ses = jsch.getSession(options.username, options.host, options.port)

        if (options.proxy != null) {
            ses.setProxy(options.proxy)
        }
        ses.serverAliveInterval = 5000
        ses.serverAliveCountMax = 5
        ses.timeout = options.connectTimeout.toInt()
        ses.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
        ses.userInfo = SSHUserInfo(options.password, options.passphrase)

        for ((k, v) in options.sessionConfig) {
            ses.setConfig(k, v)
        }

        ses.connect(options.connectTimeout.toInt())
        return ses
    }

    private fun <T> opWithFallback(primary: () -> T, fallback: () -> T): T {
        if (firstHasFailed) {
            return fallback()
        } else {
            return try {
                primary()
            } catch (e: RuntimeException) {
                if (e.message.orEmpty().contains("io.tekniq.ssh.SSH transfert protocol error")) {
                    firstHasFailed = true
                }
                fallback()
            }
        }
    }
}
