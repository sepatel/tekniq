package io.tekniq.ssh

import com.jcraft.jsch.ChannelExec
import io.tekniq.ssh.util.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.Charset

class SSHExec(cmd: String, out: ExecResult.() -> Unit, err: ExecResult.() -> Unit, val ssh: SSH) : Closeable {
    private val channel: ChannelExec = ssh.jschsession().openChannel("exec") as ChannelExec
    private val stdout: InputStream
    private val stderr: InputStream
    private val stdin: OutputStream

    init {
        channel.setCommand(cmd.toByteArray())
        stdout = channel.inputStream
        stderr = channel.errStream
        stdin = channel.outputStream
        channel.setPty(ssh.options.execWithPty)
        channel.connect(ssh.options.connectTimeout.toInt())
    }

    private val stdoutThread = InputStreamThread.Instance(channel, stdout, out, ssh.options.charset.toCharset())
    private val stderrThread = InputStreamThread.Instance(channel, stderr, err, ssh.options.charset.toCharset())
    private val timeoutThread = TimeoutManagerThread.Instance(ssh.options.timeout) {
        stdoutThread.interrupt()
        stderrThread.interrupt()
    }

    /**
     * Writes [line] to stdin.
     */
    fun giveInputLine(line: String) {
        stdin.write(line.toByteArray())
        stdin.write("\n".toByteArray())
        stdin.flush()
    }

    /**
     * Waits for all output to finish.
     */
    fun waitForEnd() {
        stdoutThread.join()
        stderrThread.join()
        if (timeoutThread.interrupted) throw InterruptedException("Timeout reached")
        close()
    }

    /**
     * Closes the exec channel and all open streams.
     */
    override fun close() {
        stdin.close()
        stdoutThread.interrupt()
        stderrThread.interrupt()
        channel.disconnect()
        timeoutThread.interrupt()
    }
}

private class TimeoutManagerThread(val timeout: Long, val todo: () -> Any) : Thread() {
    var interrupted = false

    companion object {
        fun Instance(timeout: Long, todo: () -> Any): TimeoutManagerThread {
            val newthread = TimeoutManagerThread(timeout, todo)
            newthread.start()
            return newthread
        }
    }

    override fun run() {
        if (timeout > 0) {
            try {
                sleep(timeout)
                interrupted = true
                todo()
            } catch (e: InterruptedException) {
            }
        }
    }
}

private class InputStreamThread(val channel: ChannelExec, val input: InputStream,
                                val output: ExecResult.() -> Unit, val charset: Charset) : Thread() {
    companion object {
        fun Instance(channel: ChannelExec, input: InputStream,
                     output: ExecResult.() -> Unit, charset: Charset): InputStreamThread {
            val newthread = InputStreamThread(channel, input, output, charset)
            newthread.start()
            return newthread
        }
    }

    override fun run() {
        val bufsize = 16 * 1024
        val binput = input.buffered()
        val bytes = ByteArray(bufsize)
        val buffer = ByteBuffer.allocate(bufsize)
        val appender = StringBuilder()
        var eofreached = false

        try {
            do {
                val howmany = binput.read(bytes, 0, bufsize)
                if (howmany == -1) eofreached = true
                if (howmany > 0) {
                    buffer.put(bytes, 0, howmany)
                    buffer.flip()
                    val cbOut = charset.decode(buffer)
                    buffer.compact()
                    appender.append(cbOut.toString())
                    var s = 0
                    var e: Int
                    do {
                        e = appender.indexOf("\n", s)
                        if (e >= 0) {
                            ExecPart(appender.substring(s, e)).output()
                            s = e + 1
                        }
                    } while (e != -1)
                    appender.delete(0, s)
                }
            } while (!eofreached)

            if (appender.isNotEmpty()) {
                ExecPart(appender.toString()).output()
            }
            ExecEnd(channel.exitStatus).output()
        } catch (e: InterruptedIOException) {
            ExecTimeout().output()
        } catch (e: InterruptedException) {
            ExecTimeout().output()
        }
    }

}
