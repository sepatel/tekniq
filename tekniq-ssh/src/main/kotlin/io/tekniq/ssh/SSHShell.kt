package io.tekniq.ssh

import com.jcraft.jsch.ChannelShell
import io.tekniq.ssh.operations.AllOperations
import io.tekniq.ssh.util.*
import java.io.*
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class SSHShell(ssh: SSH) : SSHScp(ssh), AllOperations {
    val options = ssh.options

    private fun createReadyMessage(): String = "ready-${System.currentTimeMillis()}"
    private val defaultPrompt = "_T-:+"
    private val customPromptGiven = options.prompt != null
    private var doInit = true
    val prompt = options.prompt ?: defaultPrompt

    val channel = ssh.jschsession().openChannel("shell") as ChannelShell
    val toServer: Producer
    val fromServer: ConsumerOutputStream

    init {
        channel.setPtyType("dumb")
        channel.setXForwarding(false)

        val pos = PipedOutputStream()
        val pis = PipedInputStream(pos)
        toServer = Producer(pos)
        channel.inputStream = pis

        fromServer = ConsumerOutputStream(customPromptGiven)
        channel.outputStream = fromServer

        channel.connect(options.connectTimeout.toInt())
    }

    /**
     * Executes [proc], providing an instance of an SSHFtp
     */
    fun <T> ftp(proc: SSHFtp.() -> T): T = SSHFtp(ssh).use { it.proc() }

    override fun close() {
        super.close()
        fromServer.close()
        toServer.close()
        channel.disconnect()
    }

    /**
     * Executes the given [cmd] and returns the result as a String
     */
    @Synchronized
    override fun execute(cmd: String): String {
        sendCommand(cmd)
        return fromServer.getResponse()
    }

    /**
     * Executes the given [cmd] and returns the result as a String, with the exit code as a Pair
     */
    @Synchronized
    override fun executeWithStatus(cmd: String): Pair<String, Int> {
        val result = execute(cmd)
        val rc = executeAndTrim("echo ${'$'}?").toInt()
        return Pair(result, rc)
    }

    /**
     * Pipes [data] into a remote [filespec]
     */
    @Synchronized
    override fun catData(data: String, filespec: String): Boolean {
        val result = execute("""touch '$filespec' >/dev/null 2>&1 ; echo ${'$'}?""").trim() == "0"
        return if (result) {
            put(data, filespec)
            val fileData = get(filespec)
            if (fileData == null) {
                false
            } else {
                fileData == data
            }
        } else {
            false
        }
    }

    fun executeWithExpects(cmd: String, expects: List<Expect>): Pair<String, Int> {
        try {
            fromServer.currentExpects = expects
            val result = execute(cmd)
            val sz = fromServer.expectsRemaining()
            return Pair(result, sz)
        } finally {
            fromServer.resetExpects()
        }
    }

    /**
     * Switch to [otherUser] via 'su -', optionally providing [password]. Returns true if the switch was successful.
     */
    fun becomeWithSU(otherUser: String, password: String? = null): Boolean {
        val curuser = whoami()
        if (curuser == "root") {
            execute("LANG=en; export LANG")
            sendCommand("su - $otherUser")
            Thread.sleep(2000)
            shellInit()
        } else if (password != null) {
            execute("LANG=en; export LANG")
            sendCommand("su - $otherUser")
            Thread.sleep(2000)
            try {
                toServer.send(password)
                Thread.sleep(1000)
            } finally {
                shellInit()
            }
        }

        return whoami() == otherUser
    }

    /**
     * Switch to [otherUser] via 'sudo su'. Returns true if the switch was successful.
     */
    fun becomeWithSudo(otherUser: String): Boolean {
        val curuser = whoami()
        if (sudoSuMinusOnlyWithoutPasswordTest()) {
            execute("LANG=en; export LANG")
            sendCommand("sudo -n su - $otherUser")
            shellInit()
        } else {
            execute("LANG=en; export LANG")
            sendCommand("sudo -S su - $otherUser")
            Thread.sleep(2000)
            try {
                if (curuser != "root") {
                    if (options.password != null) {
                        toServer.send(options.password)
                    }
                    Thread.sleep(1000)
                }
            } finally {
                shellInit()
            }
        }
        return whoami() == otherUser
    }

    /**
     * Switches to [otherUser] on the current shell session.
     * First 'su -' method is attempted, and if that fails 'sudo su -' is attempted. In the latter case, the current
     * user password will be used rather than [password]
     */
    fun become(otherUser: String, password: String? = null): Boolean = if (whoami() != otherUser) {
        switchUser(otherUser, password)
    } else {
        true
    }

    /**
     * Gets the current shell process ID
     */
    fun pid(): Int = execute("echo ${'$'}").trim().toInt()

    /**
     * Checks if `sudo su -` works without a password.
     *
     * This typical usage that maximizes compatibilities across various linux is to pipe the
     *    command to the sudo -S su -
     *
     * BUT with this usage you loose the TTY, so interactive commands such as the shell are no more possible
     *    and you directly get back to previous sh.
     *
     * Options such as -k, -A, -p ... may not be supported everywhere.
     *
     * Some notes :
     *     BAD because we want to test the su
     *       sudo -n echo OK 2>/dev/null
     *
     *     BAD because with older linux, -n option was not available
     *       sudo -n su - -c "echo OK" 2>/dev/null
     *
     *     ~GOOD but NOK if only su - is allowed
     *       echo | sudo -S su - -c echo "OK" 2>/dev/null
     *
     *     GOOD
     *       echo "echo OK" | sudo -S su - 2>/dev/null
     */
    fun sudoSuMinusOnlyWithoutPasswordTest(): Boolean {
        val testedmsg = "SUDOOK"
        return execute("""echo "echo $testedmsg" | sudo -S su - 2>/dev/null""").trim().contains(testedmsg)
    }

    /**
     * Checks if 'sudo su -' works with the current user password while preserving the TTY stdin
     */
    fun sudoSuMinusOnlyWithPasswordTest(): Boolean {
        val prompt = "password:"
        val sudosu = """SUDO_PROMPT="$prompt" sudo -S su -"""
        val expect = Expect({ it.endsWith("prompt") }, ssh.options.password.orEmpty() + "\n")
        val (_, rc) = executeWithExpects(sudosu, listOf(expect))
        val result = rc == 0 && whoami() == "root"
        if (result) execute("exit")
        return result
    }

    /**
     * Checks if 'sudo su - -c [cmd]' works transparently with or without password
     */
    fun sudoSuMinusWithCommandTest(cmd: String = "whoami"): Boolean {
        val password = options.password ?: ""
        val scriptName = ".custom-askpass-${(Random().nextDouble() * 10000000L).toLong()}"
        val script = """
            echo '$password
            #self destruction
            rm -f ${'$'}HOME/$scriptName
        """.trimIndent()
        catData(script, """${'$'}HOME/$scriptName""")
        execute("""chmod u+x ${'$'}HOME/$scriptName""")
        return execute("""${'$'}HOME/$scriptName | SUDO_PROMPT="" sudo -S su - -c "$cmd" >/dev/null 2>&1 ; echo ${'$'}?""")
                .trim() == "0"
    }

    private fun shellInit() {
        if (options.prompt == null) {
            //if no prompt is given we assume that a standard sh/bash/ksh shell is used
            val readyMessage = createReadyMessage()
            fromServer.setReadyMessage(readyMessage)
            toServer.send("unset LS_COLORS")
            toServer.send("unset EDITOR")
            toServer.send("unset PAGER")
            toServer.send("COLUMNS=500")
            toServer.send("SUDO_PS1='%s'".format(defaultPrompt)) //MUST BE REMOVED FROM THE HISTORY
            toServer.send("PS1='%s'".format(defaultPrompt)) //MUST BE REMOVED FROM THE HISTORY
            toServer.send("history -d ${'$'}((HISTCMD-3)) && history -d ${'$'}((HISTCMD-2)) && history -d ${'$'}((HISTCMD-1))") //REMOVING DEDICATED PS1 VAR EN COMMANDS
            toServer.send("echo '%s'".format(readyMessage)) //important to distinguish between the command and the result
            fromServer.waitReady()
            fromServer.getResponse() //ready response
        } else {
            fromServer.waitReady()
            fromServer.getResponse() //for the initial prompt
        }
    }

    private fun sendCommand(cmd: String) {
        if (doInit) {
            shellInit()
            doInit = false
        }
        toServer.send(cmd)
    }

    @Synchronized
    private fun switchUser(otherUser: String, password: String?): Boolean = becomeWithSU(otherUser, password) || becomeWithSudo(otherUser)

    inner class ConsumerOutputStream(val checkReady: Boolean) : OutputStream() {
        private val resultsQueue = ArrayBlockingQueue<String>(10)

        private var readyMessage = ""
        private var ready = checkReady
        private val readyQueue = ArrayBlockingQueue<String>(1)
        private var readyMessageQuotePrefix = "'$readyMessage"
        private val promptEqualPrefix = "=$prompt"

        private val consumerAppender = StringBuilder(8192)
        private val promptSize = prompt.length
        private val lastPromptChars = prompt.takeLast(2)
        private var searchForPromptIndex = 0

        var currentExpects = emptyList<Expect>()

        fun expectsRemaining(): Int = currentExpects.size

        fun resetExpects() {
            currentExpects = emptyList()
        }

        fun waitReady() {
            if (!ready) readyQueue.take()
        }

        fun hasResponse() = resultsQueue.size > 0

        fun getResponse(timeout: Long = options.timeout): String {
            if (timeout == 0L) {
                return resultsQueue.take()
            } else {
                val rval = resultsQueue.poll(timeout, TimeUnit.MILLISECONDS)
                if (rval == null) {
                    toServer.brk()
                    val output = resultsQueue.poll(5, TimeUnit.SECONDS)
                            ?: "**no return value - could not break current operation**"
                    throw SSHTimeoutException(output, "")
                } else {
                    return rval
                }
            }
        }

        fun setReadyMessage(newReadyMessage: String) {
            ready = checkReady
            readyMessage = newReadyMessage
            readyMessageQuotePrefix = "'$newReadyMessage"
        }

        override fun write(b: Int) {
            if (b != 13) {
                val ch = b.toChar()
                consumerAppender.append(ch)
                if (!ready) {
                    if (consumerAppender.endsWith(readyMessage) && !consumerAppender.endsWith(readyMessageQuotePrefix)) {
                        ready = true
                        readyQueue.put("ready")
                    }
                } else {
                    if (currentExpects.size > 0 && currentExpects.first().expectWhen(consumerAppender.toString())) {
                        toServer.write(currentExpects.first().send)
                        currentExpects = currentExpects.drop(1)
                    }
                    if (consumerAppender.endsWith(lastPromptChars)
                            && consumerAppender.endsWith(prompt)
                            && !consumerAppender.endsWith(promptEqualPrefix)) {
                        val promptIndex = consumerAppender.length - promptSize
                        val firstNlIndex = consumerAppender.indexOf("\n")
                        val result = consumerAppender.substring(firstNlIndex + 1, promptIndex)
                        resultsQueue.put(result)
                        searchForPromptIndex = 0
                        consumerAppender.setLength(0)
                    } else {
                        searchForPromptIndex = consumerAppender.length - promptSize
                        if (searchForPromptIndex < 0) searchForPromptIndex = 0
                    }
                }
            }
        }
    }


}
