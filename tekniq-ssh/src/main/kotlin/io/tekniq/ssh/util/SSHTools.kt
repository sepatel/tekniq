package io.tekniq.ssh.util

import java.io.*
import java.security.MessageDigest

object SSHTools {

    /**
     * Generates an md5 checksum from the bytes of [str]
     */
    fun md5sum(str: String): String = md5sum(ByteArrayInputStream(str.toByteArray()))

    /**
     * Generates an md5 checksum from [input]
     */
    fun md5sum(input: InputStream): String {
        val md5 = MessageDigest.getInstance("MD5")
        return md5.digest(input.readBytes()).joinToString("") {
            "%02x".format(it)
        }
    }

    /**
     * Gets the contents of a file located at [filename] as a String
     */
    fun getFile(filename: String): String = FileInputStream(filename).bufferedReader().readText()

    /**
     * Gets the contents of a file located at [filename] as a ByteArray
     */
    fun getRawFile(filename: String): ByteArray = inputStreamToByteArray(FileInputStream(filename))

    fun inputStreamToByteArray(input: InputStream): ByteArray {
        val fos = ByteArrayOutputStream(65535)
        val bfos = fos.buffered(16384)
        val bis = input.buffered()
        val buffer = ByteArray(8192)

        try {
            bis.readUntil(buffer) { it == -1 }.forEach { bfos.write(buffer, 0, it) }
        } finally {
            bfos.close()
            fos.close()
        }
        return fos.toByteArray()
    }

    /**
     * Determines the basename of a file called [name], given that it may have an extension [ext]
     */
    fun basename(name: String, ext: String) = if (name.contains(ext)) name.substring(0, name.indexOf(ext)) else name
}
