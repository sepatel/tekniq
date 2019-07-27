package io.tekniq.ssh.operations

import java.text.SimpleDateFormat
import java.util.*

interface PowerShellOperations {

    /**
     * Executes the given [cmd] and returns the result as a String
     */
    fun execute(cmd: String): String

    /**
     * Executes the given [cmd] and returns the trimmed result as a String
     */
    fun executeAndTrim(cmd: String): String = execute(cmd).trim()

    /**
     * Executes the given [cmd] and returns the trimmed result as a String
     */
    fun executeAndTrimSplit(cmd: String): List<String> = execute(cmd).trim().split("""\r?\n""".toRegex())

    /**
     * Returns the current user name on the remote system
     */
    fun whoami(): String = executeAndTrim("whoami")

    /**
     * Lists files in the current directory on the remote system
     */
    fun ls(): String = execute("ls")

    /**
     * Lists files in [dirname] on the remote system
     */
    fun ls(dirname: String): List<String> = executeAndTrimSplit("""ls "%s"""".format(dirname)).filter { it.length > 0 }

    /**
     * Lists the current working directory on the remote system
     */
    fun pwd(): String = executeAndTrim("pwd")

    /**
     * Changes to the home directory on the remote system
     */
    fun cd() = execute("cd")

    /**
     * Changes to the [path] directory on the remote system
     */
    fun cd(path: String) = execute("""cd "$path" """)

    /**
     * Gets the remote host name
     */
    fun hostname(): String = executeAndTrim("hostname")

    /**
     * Gets the remote date
     */
    fun date(): Date {
        val dateSDF = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
        val d = executeAndTrim("date -u '+%Y-%m-%d %H:%M:%S %Z00'")
        return dateSDF.parse(d)
    }

    /**
     * Gets the contents of a file called [filename] on the remote system
     */
    fun cat(filename: String) = execute("cat %s".format(filename))

    /**
     * Gets the contents of a list of files [filenames] on the remote system
     */
    fun cat(filenames: List<String>) = execute("cat %s".format(filenames.joinToString(" ")))

    /**
     * Kills remote processes having [pids], sending [signalNumber]
     */
    fun kill(pids: Iterable<Int>, signalNumber: Int = 9) = execute("""kill -$signalNumber ${pids.joinToString(" ")}""")

    /**
     * Deletes the [file]
     */
    fun rm(file: String) = rm(listOf(file))

    /**
     * Deletes all [files]
     */
    fun rm(files: Iterable<String>) = execute("""rm -f ${files.joinToString("' '", "'", "'")}""")

    /**
     * Deletes the [dir]
     */
    fun rmdir(dir: String): Boolean = rmdir(listOf(dir))

    /**
     * Deletes all [dirs]
     */
    fun rmdir(dirs: Iterable<String>): Boolean = executeAndTrim("""rmdir ${dirs.joinToString("' '", "'", "'")} && echo ${'$'}?""") == "0"
}
