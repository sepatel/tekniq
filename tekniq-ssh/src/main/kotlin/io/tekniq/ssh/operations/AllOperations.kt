package io.tekniq.ssh.operations

import java.io.File

interface AllOperations : ShellOperations, TransferOperations {

    /**
     * Recursively copy a [remote] directory to a local [dest]
     */
    fun rreceive(remote: String, dest: File) {
        fun worker(curremote: String, curdest: File) {
            if (!isDirectory(curremote)) {
                receive(curremote, curdest)
            } else {
                var newRemote: String
                var newDest: File
                ls(curremote).forEach { found ->
                    newRemote = "$curremote/$found"
                    newDest = File(curdest, found)
                    curdest.mkdirs()
                    worker(newRemote, newDest)
                }
            }
        }
        worker(remote, dest)
    }

    /**
     * Recursively copy a local directory at [src] to a [remote] destination
     */
    fun rsend(src: File, remote: String) {
        fun worker(cursrc: File, curremote: String) {
            if (!cursrc.isDirectory) {
                send(cursrc, curremote)
            } else {
                var newSrc: File
                var newRemote: String
                cursrc.listFiles().forEach { found ->
                    newSrc = File(cursrc, found.name)
                    newRemote = "$curremote/${found.name}"
                    mkdir(curremote)
                    worker(newSrc, newRemote)
                }
            }
        }
        worker(src, remote)
    }
}
