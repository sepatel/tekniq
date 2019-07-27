package io.tekniq.ssh

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import io.tekniq.ssh.operations.TransferOperations
import io.tekniq.ssh.util.SSHTools
import io.tekniq.ssh.util.toCharset
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*

class SSHFtp(val ssh: SSH) : TransferOperations, Closeable {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private val channel: ChannelSftp = ssh.jschsession().openChannel("sftp") as ChannelSftp

    init {
        channel.connect(ssh.options.connectTimeout.toInt())
    }

    /**
     * Returns possible content from a remote file called [remoteFilename] as a string
     */
    override fun get(remoteFilename: String): String? {
        return try {
            channel.get(remoteFilename).bufferedReader().readText()
        } catch (se: SftpException) {
            null
        } catch (ioe: IOException) {
            null
        }
    }

    /**
     * Returns possible content from a remote file called [remoteFilename] as an array of bytes
     */
    override fun getBytes(remoteFilename: String): ByteArray? {
        return try {
            SSHTools.inputStreamToByteArray(channel.get(remoteFilename))
        } catch (se: SftpException) {
            null
        } catch (ioe: IOException) {
            null
        }
    }

    /**
     * Copies the remote file called [remoteFilename] to the local [outputStream]
     */
    override fun receive(remoteFilename: String, outputStream: OutputStream) {
        try {
            channel.get(remoteFilename, outputStream)
        } catch (se: SftpException) {
            if (se.id == 2) {
                logger.warn("File $remoteFilename does not exist")
            }
            throw se
        } catch (ioe: IOException) {
            logger.error("Cannot receive $remoteFilename", ioe)
            throw ioe
        } catch (e: Exception) {
            logger.error("Cannot receive $remoteFilename", e)
            throw e
        } finally {
            outputStream.close()
        }
    }

    /**
     * Uploads string [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun put(data: String, remoteDestination: String) {
        channel.put(ByteArrayInputStream(data.toByteArray(ssh.options.charset.toCharset())), remoteDestination)
    }

    /**
     * Uploads byte array [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    override fun putBytes(data: ByteArray, remoteDestination: String) {
        channel.put(ByteArrayInputStream(data), remoteDestination)
    }

    /**
     * Uploads input stream [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten. [howmany] is ignored in this implementation.
     */
    override fun putFromStream(data: InputStream, howmany: Int, remoteDestination: String) {
        putFromStream(data, remoteDestination)
    }

    /**
     * Copies a [localFile] to a remote file at [remoteDestination]
     */
    override fun send(localFile: File, remoteDestination: String) {
        channel.put(FileInputStream(localFile), remoteDestination)
    }

    /**
     * Uploads input stream [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    fun putFromStream(data: InputStream, remoteDestination: String) {
        channel.put(data, remoteDestination)
    }

    /**
     * Gets a remote file called [filename] as a stream
     */
    fun getStream(filename: String): InputStream? {
        return try {
            channel.get(filename)
        } catch (se: SftpException) {
            null
        } catch (ioe: IOException) {
            null
        }
    }

    /**
     * Renames a remote file or directory from [origin] to [dest]
     */
    fun rename(origin: String, dest: String) = channel.rename(origin, dest)

    /**
     * Lists contents of remote directory [path]
     */
    fun ls(path: String): List<ChannelSftp.LsEntry> {
        return channel.ls(path)
                .map { it as ChannelSftp.LsEntry }
    }

    /**
     * Removes a file at [path] from remote system
     */
    fun rm(path: String) = channel.rm(path)

    /**
     * Removes a directory at [path] from remote system
     */
    fun rmdir(path: String) = channel.rmdir(path)

    /**
     * Creates a directory at [path] on remote system
     */
    fun mkdir(path: String) = channel.mkdir(path)

    /**
     * Changes working directory to [path] on remote system
     */
    fun cd(path: String) = channel.cd(path)

    /**
     * Shows current working directory on remote system
     */
    fun pwd() = channel.pwd()

    /**
     * Converts a remote [path] to an absolute path
     */
    fun realpath(path: String): String = channel.realpath(path)

    override fun close() {
        channel.quit()
        channel.disconnect()
    }

}
