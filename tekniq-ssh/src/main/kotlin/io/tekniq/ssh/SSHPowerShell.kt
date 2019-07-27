package io.tekniq.ssh

import com.jcraft.jsch.ChannelShell
import io.tekniq.ssh.operations.PowerShellOperations
import io.tekniq.ssh.util.Producer
import io.tekniq.ssh.util.SSHTimeoutException
import java.io.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class SSHPowerShell(val ssh: SSH) : PowerShellOperations, Closeable {
    private var doInit = true
    private val defaultPrompt = """_T-:+"""
    val prompt = ssh.options.prompt ?: defaultPrompt
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

        fromServer = ConsumerOutputStream()
        channel.outputStream = fromServer

        channel.connect(ssh.options.connectTimeout.toInt())
    }

    override fun close() {
        fromServer.close()
        toServer.close()
        channel.disconnect()
    }

    /**
     * Executes the given [cmd] and returns the result as a String
     */
    @Synchronized
    override fun execute(cmd: String): String {
        sendCommand(cmd.replace('\n', ' '))
        return fromServer.getResponse()
    }

    private fun shellInit() {
        toServer.send("""function prompt {"$prompt"}""")

        //Must read output twice to get through the set prompt command echo and then the initial prompt
        fromServer.getResponse()
        fromServer.getResponse()
    }

    private fun sendCommand(cmd: String) {
        if (doInit) {
            shellInit()
            doInit = false
        }
        toServer.send(cmd)
    }

    inner class ConsumerOutputStream : OutputStream() {
        private val resultsQueue = ArrayBlockingQueue<String>(10)
        private val consumerAppender = StringBuilder(8192)
        private val promptSize = prompt.length

        fun hasResponse() = resultsQueue.size > 0

        fun getResponse(timeout: Long = ssh.options.timeout): String {
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

        override fun write(b: Int) {
            if (b != 13) { //CR removed... CR is always added by JSch
                val ch = b.toChar()
                consumerAppender.append(ch)

                if (consumerAppender.endsWith(prompt)) {
                    val promptIndex = consumerAppender.length - promptSize
                    val firstNIndex = consumerAppender.indexOf("\n")
                    val result = consumerAppender.substring(firstNIndex + 1, promptIndex)
                    resultsQueue.put(result)
                    consumerAppender.setLength(0)
                }
            }
        }
    }
}
