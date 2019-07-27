package io.tekniq.ssh.operations

import io.tekniq.ssh.util.SSHTools
import java.io.File

interface CommonOperations {

    /**
     * Generates an md5sum for the local file called [filename]
     */
    fun localmd5sum(filename: String): String? {
        val file = File(filename)

        return if (file.exists()) SSHTools.md5sum(file.inputStream()) else null
    }
}
