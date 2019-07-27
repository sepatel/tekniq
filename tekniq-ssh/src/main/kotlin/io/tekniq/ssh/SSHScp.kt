package io.tekniq.ssh

import com.jcraft.jsch.ChannelExec
import io.tekniq.ssh.operations.TransferOperations
import io.tekniq.ssh.util.readUntil
import io.tekniq.ssh.util.toCharset
import java.io.*

open class SSHScp(val ssh: SSH) : TransferOperations, Closeable {

    /**
     * Returns possible content from a remote file called [remoteFilename] as a string
     */
    override fun get(remoteFilename: String): String? {
        return getBytes(remoteFilename)?.let {
            String(it, ssh.options.charset.toCharset())
        }
    }

    /**
     * Returns possible content from a remote file called [remoteFilename] as an array of bytes
     */
    override fun getBytes(remoteFilename: String): ByteArray? {
        val filesBuffer = mutableMapOf<String, ByteArrayOutputStream>()
        fun filenameToOutputStream(filename: String): ByteArrayOutputStream {
            val newout = ByteArrayOutputStream()
            filesBuffer.put(filename, newout)
            return newout
        }

        return remoteFileToOutputStream(remoteFilename, ::filenameToOutputStream).let {
            when (it) {
                0 -> null
                1 -> filesBuffer.values.first().toByteArray()
                else -> throw RuntimeException("Want one file, but several files were found (${filesBuffer.keys.joinToString()})")
            }
        }

    }

    /**
     * Copies the remote file called [remoteFilename] to the local [outputStream]
     */
    override fun receive(remoteFilename: String, outputStream: OutputStream) {
        fun filenameToOutputStream(filename: String) = outputStream

        return remoteFileToOutputStream(remoteFilename, ::filenameToOutputStream).let {
            when (it) {
                0 -> throw RuntimeException("Remote file name '$remoteFilename' not found")
                1 -> return
                else -> throw RuntimeException("Want one file, but several files were found for '$remoteFilename'")
            }
        }
    }

    /**
     * Uploads string [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun put(data: String, remoteDestination: String) {
        putBytes(data.toByteArray(ssh.options.charset.toCharset()), remoteDestination)
    }

    /**
     * Uploads byte array [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun putBytes(data: ByteArray, remoteDestination: String) {
        val sz = data.size.toLong()
        val linput = ByteArrayInputStream(data)
        val parts = remoteDestination.split("/")
        val rfilename = parts.last()
        val rDirectory = if (parts.size == 1) "." else parts.dropLast(1).joinToString("/")

        inputStreamToRemoteFile(linput, sz, rfilename, rDirectory)
    }

    /**
     * Uploads [howmany] bytes of input stream [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun putFromStream(data: InputStream, howmany: Int, remoteDestination: String) {
        val parts = remoteDestination.split("/")
        val rfilename = parts.last()
        val rDirectory = if (parts.size == 1) "." else parts.dropLast(1).joinToString("/")

        inputStreamToRemoteFile(data, howmany.toLong(), rfilename, rDirectory)
    }

    /**
     * Copies a [localFile] to a remote file at [remoteDestination]
     */
    override fun send(localFile: File, remoteDestination: String) {
        val sz = localFile.length()
        val linput = FileInputStream(localFile)
        val parts = remoteDestination.split("/".toRegex(), -1)
        val rfilename = if (parts.last().length == 0) localFile.name else parts.last()
        val rDirectory = if (parts.size == 1) "." else parts.dropLast(1).joinToString("/")

        inputStreamToRemoteFile(linput, sz, rfilename, rDirectory)
    }

    /**
     * Uploads [datasize] bytes of local stream [localinput] to a remote location [remoteDirectory]/[remoteFilename]
     */
    fun inputStreamToRemoteFile(
            localinput: InputStream,
            datasize: Long,
            remoteFilename: String,
            remoteDirectory: String
    ) {
        val ch = ssh.jschsession().openChannel("exec") as ChannelExec
        try {
            ch.setCommand("""scp -p -t "%s" """.format(remoteDirectory))
            val sin = ch.inputStream.buffered()
            val sout = ch.outputStream
            ch.connect(ssh.options.connectTimeout.toInt())

            checkAck(sin)

            //send "C0644 filesize filename", where filename should not include '/'
            val command = "C0644 %d %s\n".format(datasize, remoteFilename) //TODO take into account remote file rights
            sout.write(command.toByteArray("US-ASCII".toCharset()))
            sout.flush()

            checkAck(sin)

            val bis = localinput.buffered()
            var writtenBytes = 0
            while (writtenBytes < datasize) {
                val c = bis.read()
                if (c >= 0) {
                    sout.write(c)
                    writtenBytes++
                }
            }
            bis.close()

            //send '\0'
            sout.write(0x00)
            sout.flush()

            checkAck(sin)
        } finally {
            if (ch.isConnected) {
                ch.disconnect()
            }
        }
    }

    /**
     * Lookup remote files with the mask [remoteFilenameMask] and send their contents to an OutputStream created via [outputStreamBuilder]
     */
    fun remoteFileToOutputStream(
            remoteFilenameMask: String,
            outputStreamBuilder: (String) -> OutputStream
    ): Int {
        val ch = ssh.jschsession().openChannel("exec") as ChannelExec

        try {
            ch.setCommand("""scp -f "$remoteFilenameMask" """)
            val sin = ch.inputStream.buffered()
            val sout = ch.outputStream
            ch.connect(ssh.options.connectTimeout.toInt())

            sout.write(0)
            sout.flush()

            var count = 0
            val buf = StringBuilder() // Warning : Mutable state, take care
            fun bufAppend(x: Int) {
                buf.append(x.toChar())
            }

            fun bufReset() {
                buf.setLength(0)
            }

            fun bufStr() = buf.toString()

            while (checkAck(sin) == 'C'.toInt()) {
                val fileRights = ByteArray(5)
                sin.read(fileRights, 0, 5)

                bufReset()
                sin.readUntil { it == ' '.toInt() }.forEach { bufAppend(it) }
                val fz = bufStr().toLong()

                bufReset()
                sin.readUntil { it == 0x0a }.forEach { bufAppend(it) }
                val filename = bufStr()

                println(remoteFilenameMask + " " + count + " " + String(fileRights) + " '" + filename + "' #" + fz)

                sout.write(0)
                sout.flush()

                val fos = BufferedOutputStream(outputStreamBuilder(filename), 8192)

                var writtenBytes = 0L
                while (writtenBytes < fz) {
                    val c = sin.read()
                    if (c >= 0) {
                        fos.write(c)
                        writtenBytes++
                    }
                }
                fos.close()

                count++

                checkAck(sin)
                sout.write(0)
                sout.flush()
            }

            return count
        } finally {
            if (ch.isConnected) {
                ch.disconnect()
            }
        }
    }

    override fun close() {}

    private fun checkAck(input: InputStream): Int {
        val consumeMessage = {
            val sb = StringBuffer()
            input.buffered().readUntil {
                it == -1 || it == '\n'.toInt()
            }.forEach { x -> sb.append(x.toChar()) }
        }

        val readVal = input.read()
        return when (readVal) {
            1 -> throw RuntimeException("io.tekniq.ssh.SSH transfert protocol error " + consumeMessage())
            2 -> throw RuntimeException("io.tekniq.ssh.SSH transfert protocol fatal error " + consumeMessage())
            else -> readVal
        }
    }

}
