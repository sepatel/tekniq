package io.tekniq.ssh.operations

import io.tekniq.ssh.util.*
import java.text.SimpleDateFormat
import java.util.*

interface ShellOperations : CommonOperations {
    /**
     * Executes the given [cmd] and returns the result as a String
     */
    fun execute(cmd: String): String

    /**
     * Executes the given [cmd] and returns the result as a String, with the exit code as a Pair
     */
    fun executeWithStatus(cmd: String): Pair<String, Int>

    /**
     * Executes the given [cmd] and returns the trimmed result as a String
     */
    fun executeAndTrim(cmd: String): String = execute(cmd).trim()

    /**
     * Executes the given [cmd] and returns the trimmed result as a String
     */
    fun executeAndTrimSplit(cmd: String): List<String> = execute(cmd).trim().split("\r?\n".toRegex())

    /**
     * Pipes [data] into a remote [filespec]
     */
    fun catData(data: String, filespec: String): Boolean

    /**
     * Disable shell history, the goal is to not add noises to your shell history, to
     * keep your shell commands history clean.
     */
    fun disableHistory() {
        execute("unset HISTFILE")
        execute("HISTSIZE=0")
    }

    /**
     * Retrieves the size of the remote file called [filename] in bytes
     */
    fun fileSize(filename: String): Long? {
        val result = genoptcmd("""ls -ld "$filename" """)
        if (result != null) {
            return (result.split("""\s+""".toRegex())[4]).toLong()
        } else {
            return null
        }
    }

    /**
     * Determines the last modified date of the remote file called [filename], accounting for TZ
     */
    fun lastModified(filename: String): Date? = when (osid()) {
        is Linux -> {
            val result = genoptcmd("""stat -c '%y' '$filename' """)
            if (result == null) null else lmLinuxFix(result)
        }
        is Darwin -> {
            val lmDarwinSDF = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
            val result = genoptcmd("""stat -t '%Y-%m-%d %H:%M:%S %Z' -x '$filename' | grep Modify""")
            if (result != null) {
                lmDarwinSDF.parse(result.split(":".toRegex(), 2).drop(1).joinToString(",").trim())
            } else {
                null
            }
        }
        is AIX -> {
            val lmAixSDF = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
            val result = genoptcmd("""istat '$filename' | grep "Last modified" """)
            if (result != null) {
                lmAixSDF.parse(result.split(":".toRegex(), 2)
                        .drop(1)
                        .joinToString(",")
                        .trim()
                        .replaceFirst("DFT", "CET")
                )
            } else {
                null
            }
        }
        else -> null
    }

    /**
     * Finds the file size (in kilobytes) of the remote file tree called [filename]
     */
    fun du(filename: String): Long? = genoptcmd("""du -k "$filename" | tail -1""")
            ?.split("""\s+""".toRegex(), 2)
            ?.get(0)
            ?.toLong()

    /**
     * Generates an md5sum for the remote file called [filename]
     */
    fun md5sum(filename: String): String? = when (osid()) {
        is Darwin -> genoptcmd("""md5 "$filename" """)
                ?.split("=".toRegex(), 2)
                ?.get(1)
                ?.trim()
        is AIX -> genoptcmd("""csum -h MD5 "$filename" """)
                ?.split("""\s+""".toRegex())
                ?.get(0)
                ?.trim()
        else -> genoptcmd("""md5sum "$filename" """)
                ?.split("""\s+""".toRegex())
                ?.get(0)
                ?.trim()
    }

    /**
     * Generates an sha1sum for the remote file called [filename]
     */
    fun sha1sum(filename: String): String? = when (osid()) {
        is Darwin -> genoptcmd("""shasum "$filename" """)
                ?.split("""\s+""".toRegex(), 2)
                ?.get(0)
                ?.trim()
        is AIX -> genoptcmd("""csum -h SHA1 "$filename" """)
                ?.split("""\s+""".toRegex())
                ?.get(0)
                ?.trim()
        else -> genoptcmd("""sha1sum "$filename" """)
                ?.split("""\s+""".toRegex())
                ?.get(0)
                ?.trim()
    }

    /**
     * Returns the current user name on the remote system
     */
    fun whoami(): String = executeAndTrim("whoami")

    /**
     * Returns the *nix system name (io.tekniq.ssh.util.Linux, AIX, io.tekniq.ssh.util.SunOS, ...)
     */
    fun uname(): String = executeAndTrim("""uname 2>/dev/null""")

    /**
     * Returns the *nix os name (linux, aix, sunos, darwin, ...)
     */
    fun osname(): String = uname().toLowerCase()

    /**
     * Returns the *nix os name (linux, aix, sunos, darwin, ...) as a representative object
     */
    fun osid(): OS = when (osname()) {
        "linux" -> Linux()
        "aix" -> AIX()
        "darwin" -> Darwin()
        "sunos" -> SunOS()
        else -> UnknownOS()
    }

    /**
     * Returns a key-value map of remote environment variables
     */
    fun env(): Map<String, String> {
        val envRE = """([^=]+)=(.*)""".toRegex()
        val lines = execute("env").split("\n")

        return mapOf(*lines.filter {
            envRE.matches(it)
        }.map { line ->
            val destructured = envRE.find(line)!!.destructured
            Pair(destructured.component1(), destructured.component2())
        }.toTypedArray())
    }

    /**
     * Lists files in the current directory on the remote system
     */
    fun ls(): List<String> = ls(".")

    /**
     * Lists files in [dirname] on the remote system
     */
    fun ls(dirname: String): List<String> = executeAndTrimSplit("""ls "%s" | cat """.format(dirname)).filter { it.length > 0 }

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
        val d = executeAndTrim("date -u '+%Y-%m-%d %H:%M:%S %Z'")
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
     * Finds file paths modified after [after] within the [root] directory.
     */
    fun findAfterDate(root: String, after: Date): List<String> {
        fun elapsedInMinutes(thatDate: Date): Long = (Date().time - thatDate.time) / 1000 / 60

        val finder = when (osid()) {
            is Linux, is AIX -> """find %s -follow -type f -mmin '-%d' 2>/dev/null"""
            is SunOS -> throw RuntimeException("SunOS not supported - find command does not support -mmin parameter")
            else -> """find %s -type f -mmin '-%d' 2>/dev/null"""
        }

        val findcommand = finder.format(root, elapsedInMinutes(after))
        return executeAndTrimSplit(findcommand)
    }

    /**
     * Tests a generic [condition], via the 'test' command
     */
    fun test(condition: String): Boolean {
        val cmd = """test %s ; echo $?""".format(condition)
        return executeAndTrim(cmd).toInt() == 0
    }

    /**
     * Checks if [filename] exists
     */
    fun exists(filename: String): Boolean = testFile("-e", filename)

    /**
     * Checks if [filename] does not exist
     */
    fun notExists(filename: String): Boolean = !exists(filename)

    /**
     * Checks if [filename] is a directory
     */
    fun isDirectory(filename: String): Boolean = testFile("-d", filename)

    /**
     * Checks if [filename] is a regular file
     */
    fun isFile(filename: String): Boolean = testFile("-f", filename)

    /**
     * Checks if [filename] is executable
     */
    fun isExecutable(filename: String): Boolean = testFile("-x", filename)

    /**
     * Lists active processes on a unix like system
     */
    fun ps(): List<SystemProcess> {
        fun processLinesToMap(pscmd: String, format: String): List<Map<String, String>> {
            val fields = format.split(",")
            val rval = executeAndTrimSplit(pscmd)
            return rval
                    .drop(1)
                    .map { it.trim() }
                    .map { it.split("""\s+""".toRegex(), fields.size) }
                    .filter { it.size == fields.size }
                    .map { fields.zip(it) }
                    .map { it.toMap() }
        }

        when (osid()) {
            is Linux -> {
                val format = "pid,ppid,user,state,vsz,rss,etime,cputime,cmd"
                val cmd = "ps -eo $format | grep -v grep | cat"

                return processLinesToMap(cmd, format).map { m ->
                    LinuxProcess(
                            m.getOrDefault("pid", "-1").toInt(),
                            m.getOrDefault("ppid", "-1").toInt(),
                            m.get("user").orEmpty(),
                            m.get("cmd").orEmpty(),
                            ProcessStatePlus.linuxSpec(m.get("state").orEmpty()),
                            m.getOrDefault("rss", "-1").toInt(),
                            m.getOrDefault("vsz", "-1").toInt(),
                            ProcessTime.fromString(m.get("etime").orEmpty()),
                            ProcessTime.fromString(m.get("cputime").orEmpty())
                    )
                }
            }
            is AIX -> {
                val format = "pid,ppid,ruser,args"
                val cmd = "ps -eo $format | grep -v grep | cat"
                return processLinesToMap(cmd, format).map { m ->
                    AIXProcess(
                            m.getOrDefault("pid", "-1").toInt(),
                            m.getOrDefault("ppid", "-1").toInt(),
                            m.get("ruser").orEmpty(),
                            m.get("args").orEmpty()
                    )
                }
            }
            is SunOS -> {
                val format = "pid,ppid,ruser,args"
                val cmd = "ps -eo $format | grep -v grep | cat"
                return processLinesToMap(cmd, format).map { m ->
                    SunOSProcess(
                            m.getOrDefault("pid", "-1").toInt(),
                            m.getOrDefault("ppid", "-1").toInt(),
                            m.get("ruser").orEmpty(),
                            m.get("args").orEmpty()
                    )
                }
            }
            is Darwin -> {
                val format = "pid,ppid,user,state,vsz,rss,etime,cputime,command"
                val cmd = "ps -eo $format | grep -v grep | cat"

                return processLinesToMap(cmd, format).map { m ->
                    DarwinProcess(
                            m.getOrDefault("pid", "-1").toInt(),
                            m.getOrDefault("ppid", "-1").toInt(),
                            m.get("user").orEmpty(),
                            m.get("cmd").orEmpty(),
                            ProcessStatePlus.darwinSpec(m.get("state").orEmpty()),
                            m.getOrDefault("rss", "-1").toInt(),
                            m.getOrDefault("vsz", "-1").toInt(),
                            ProcessTime.fromString(m.get("etime").orEmpty()),
                            ProcessTime.fromString(m.get("cputime").orEmpty())
                    )
                }
            }
            else -> {
                return emptyList()
            }
        }
    }

    /**
     * Gets PID of all remote processes whose command line matches [regex]
     */
    fun pidof(regex: Regex): List<Int> = ps()
            .filter { p -> regex.containsMatchIn(p.cmd) }
            .map { p -> p.pid }

    /**
     * File system remaining space under [path] in MB
     */
    fun fsFreeSpace(path: String): Double? = when (osid()) {
        is Linux, is AIX, is Darwin -> executeAndTrimSplit("""df -Pm '$path'""")
                .drop(1)
                .firstOrNull()?.let {
                    it.split("""\s+""").drop(3).firstOrNull()?.toDouble()
                }
        else -> null
    }

    /**
     * Gets the permissions string for the file at [path] (e.g. 'drwxr-xr-x')
     */
    fun fileRights(path: String): String? = when (osid()) {
        is Linux -> executeAndTrim("test '$path' && stat --format '%A' '$path'").let {
            if (it.isEmpty()) null else it
        }
        is AIX, is Darwin -> executeAndTrim("test '$path' && ls -lad '$path'").let {
            if (it.isEmpty()) null else it.split("""\s+""".toRegex(), 2).firstOrNull()
        }
        else -> null
    }

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

    /**
     * Gets server architecture string
     */
    fun arch() = execute("arch")

    /**
     * Creates a new directory at [dirname]
     */
    fun mkdir(dirname: String): Boolean = executeAndTrim("""mkdir -p '$dirname' && echo ${'$'}?""") == "0"

    /**
     * Creates a new directory at [dirname] and changes to that directory
     */
    fun mkcd(dirname: String): Boolean = executeAndTrim("""mkdir -p '$dirname' && cd '$dirname' && echo ${'$'}?""") == "0"

    /**
     * Gets host uptime. Example formats:
     * Linux  : 21:34:17 up 33 min,  5 users,  load average: 0.18, 0.27, 0.30
     *          21:29:38 up 473 days, 22:21,  1 user,  load average: 0.09, 0.04, 0.00
     * Darwin : 21:28  up 53 mins, 3 users, load averages: 1.40 1.49 1.52
     */
    fun uptime(): String = execute("(LANG=en; uptime")

    /**
     * Gets the directory portion of [filename]
     */
    fun dirname(filename: String): String = executeAndTrim("""dirname "$filename"""")

    /**
     * Gets the base filename portion of [filename]
     */
    fun basename(filename: String): String = executeAndTrim("""basename "$filename"""")

    /**
     * Gets the base filename portion of [filename], removing [suffix]
     */
    fun basename(filename: String, suffix: String): String = executeAndTrim("""basename "$filename" "$suffix"""")

    /**
     * Touches [files]
     */
    fun touch(files: Iterable<String>) = execute("""touch ${files.joinToString("' '", "'", "'")}""")

    /**
     * Gets user id information
     */
    fun id(): String = execute("id")

    /**
     * Echoes [message] and returns the output
     */
    fun echo(message: String): String = execute("""echo $message""")

    /**
     * Checks if the server is alive by running an echo command
     */
    fun alive(): Boolean = echo("ALIVE").contains("ALIVE")

    /**
     * Gets location of [command] on the remote system, based on the current PATH
     */
    fun which(command: String): String? = execute("""which $command""").trim().let {
        if (it.isEmpty()) null else it
    }

    private fun lmLinuxFix(input: String): Date? {
        val lmLinuxDateRE = """(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2})[.,](\d+) (.*)""".toRegex()
        val lmLinuxSDF = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S Z") // TAKE CARE only take first 3 digits of 432070011 !!!!
        val matchResult = lmLinuxDateRE.find(input)

        if (matchResult != null) {
            val (date, time, millis, tz) = matchResult.destructured
            val shorten = millis.take(3)
            return lmLinuxSDF.parse("$date $time.$shorten $tz")
        } else {
            return null
        }
    }

    private fun genoptcmd(cmd: String): String? {
        val result = executeAndTrim("""%s 2>/dev/null""".format(cmd))
        return if (result.isEmpty()) null else result
    }

    private fun testFile(testopt: String, filename: String): Boolean {
        val cmd = """test $testopt "$filename" ; echo $?"""
        val rval = executeAndTrim(cmd)
        return rval.toInt() == 0
    }
}
