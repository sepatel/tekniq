package io.tekniq.ssh.operations

import java.io.*

interface TransferOperations : CommonOperations {

    /**
     * Returns possible content from a remote file called [remoteFilename] as a string
     */
    fun get(remoteFilename: String): String?

    /**
     * Returns possible content from a remote file called [remoteFilename] as an array of bytes
     */
    fun getBytes(remoteFilename: String): ByteArray?

    /**
     * Copies the remote file called [remoteFilename] to the local [outputStream]
     */
    fun receive(remoteFilename: String, outputStream: OutputStream)

    /**
     * Uploads string [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    fun put(data: String, remoteDestination: String)

    /**
     * Uploads byte array [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    fun putBytes(data: ByteArray, remoteDestination: String)

    /**
     * Uploads [howmany] bytes of input stream [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    fun putFromStream(data: InputStream, howmany: Int, remoteDestination: String)

    /**
     * Copies a [localFile] to a remote file at [remoteDestination]
     */
    fun send(localFile: File, remoteDestination: String)

    /**
     * Copies the remote file called [remoteFilename] to the local [localFile]
     */
    fun receive(remoteFilename: String, localFile: File) {
        receive(remoteFilename, FileOutputStream(localFile))
    }

    /**
     * Copies the remote file called [remoteFilename] to a local file called [localFilename]
     */
    fun receive(remoteFilename: String, localFilename: String) {
        receive(remoteFilename, File(localFilename))
    }

    /**
     * Copies the remote file called [remoteFilename] to a local file with the same name
     */
    fun receive(filename: String) {
        receive(filename, File(filename))
    }

    /**
     * Copies a local file at [fromLocalFilename] to a remote file at [remoteDestination]
     */
    fun send(fromLocalFilename: String, remoteDestination: String) {
        send(File(fromLocalFilename), remoteDestination)
    }

    /**
     * Copies a local file at [filename] to a remote file with the same name
     */
    fun send(filename: String) {
        send(File(filename), filename)
    }

    private fun compressedCheck(filename: String): String? {
        val GZ = """.*[.]gz$""".toRegex()
        val XZ = """.*[.]xz$""".toRegex()
        val BZ = """.*[.](?:(?:bz2)|(?:bzip2))""".toRegex()

        val f = filename.toLowerCase()

        if (f.matches(GZ)) {
            return "gz"
        } else if (f.matches(BZ)) {
            return "bz"
        } else if (f.matches(XZ)) {
            return "xz"
        } else {
            return null
        }
    }
}
