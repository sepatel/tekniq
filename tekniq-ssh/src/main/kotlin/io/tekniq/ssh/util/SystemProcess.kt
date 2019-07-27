package io.tekniq.ssh.util

data class ProcessTime(val days: Int, val hours: Int, val minutes: Int, val seconds: Int) {
    val elapsedInSeconds = days * 24 * 3600 + hours * 3600 + minutes * 60 + seconds

    companion object {
        fun fromString(spec: String): ProcessTime {
            val re1 = """(\d+)""".toRegex()
            val re2 = """(\d+):(\d+)""".toRegex()
            val re3 = """(\d+):(\d+):(\d+)""".toRegex()
            val re4 = """(\d+)-(\d+):(\d+):(\d+)""".toRegex()

            return when {
                re1.matches(spec) -> re1.find(spec)!!.let {
                    val parts = it.groupValues.drop(1).map { it.toInt() }
                    ProcessTime(0, 0, 0, parts[0])
                }
                re2.matches(spec) -> re2.find(spec)!!.let {
                    val parts = it.groupValues.drop(1).map { it.toInt() }
                    ProcessTime(0, 0, parts[0], parts[1])
                }
                re3.matches(spec) -> re3.find(spec)!!.let {
                    val parts = it.groupValues.drop(1).map { it.toInt() }
                    ProcessTime(0, parts[0], parts[1], parts[2])
                }
                re4.matches(spec) -> re4.find(spec)!!.let {
                    val parts = it.groupValues.drop(1).map { it.toInt() }
                    ProcessTime(parts[0], parts[1], parts[2], parts[3])
                }
                else -> ProcessTime(0, 0, 0, 0)
            }

        }
    }
}

sealed class ProcessState(val name: String)
sealed class ProcessStatePlus(name: String, val extra: String) : ProcessState(name) {
    companion object {
        private val linuxStates = mapOf(
                'D' to "UninterruptibleSleep",
                'R' to "Running",
                'S' to "InterruptibleSleep",
                'T' to "Stopped",
                'W' to "Paging",
                'X' to "Dead",
                'Z' to "Zombie"
        )
        private val darwinStates = mapOf(
                'I' to "Idle", //sleeping for longer than 20 seconds
                'R' to "Running",
                'S' to "Sleeping", //sleeping for less than 20 seconds
                'T' to "Stopped",
                'U' to "UninterruptibleSleep",
                'Z' to "Zombie"
        )

        fun linuxSpec(spec: String): LinuxProcessState {
            val name = linuxStates.getOrDefault(spec.get(0), "UnknownState")
            val extra = if (spec.length > 0) spec.substring(1) else ""
            return LinuxProcessState(name, extra)
        }

        fun darwinSpec(spec: String): DarwinProcessState {
            val name = darwinStates.getOrDefault(spec.get(0), "UnknownState")
            val extra = if (spec.length > 0) spec.substring(1) else ""
            return DarwinProcessState(name, extra)
        }
    }
}

class LinuxProcessState(name: String, extra: String) : ProcessStatePlus(name, extra)
class DarwinProcessState(name: String, extra: String) : ProcessStatePlus(name, extra)

sealed class SystemProcess(
        val pid: Int,
        val ppid: Int,
        val user: String,
        val cmdline: String
) {
    private val tokens = cmdline.split("""\s+""".toRegex()).filter { it.length > 0 }
    val cmd = if (tokens.size > 0) tokens.first() else ""
    val args = if (tokens.size > 0) tokens.drop(1) else emptyList()
}

class AIXProcess(pid: Int, ppid: Int, user: String, cmdline: String) : SystemProcess(pid, ppid, user, cmdline)
class SunOSProcess(pid: Int, ppid: Int, user: String, cmdline: String) : SystemProcess(pid, ppid, user, cmdline)

class LinuxProcess(
        pid: Int,
        ppid: Int,
        user: String,
        cmdline: String,
        val state: LinuxProcessState,
        val rss: Int, //Resident memory size
        val vsz: Int, //Virtual memory size
        val etime: ProcessTime, //Elapsed time since start [DD-]hh:mm:ss
        val cputime: ProcessTime //CPU time used since start [[DD-]hh:]mm:ss
) : SystemProcess(pid, ppid, user, cmdline)

class DarwinProcess(
        pid: Int,
        ppid: Int,
        user: String,
        cmdline: String,
        val state: DarwinProcessState,
        val rss: Int, //Resident memory size
        val vsz: Int, //Virtual memory size
        val etime: ProcessTime, //Elapsed time since start [DD-]hh:mm:ss
        val cputime: ProcessTime //CPU time used since start [[DD-]hh:]mm:ss
) : SystemProcess(pid, ppid, user, cmdline)
