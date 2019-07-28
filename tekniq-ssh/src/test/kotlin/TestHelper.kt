import io.tekniq.ssh.SSHOptions

object TestHelper {
    val opts = SSHOptions(host = "vultr.vm",
            knownHostsFile = "~/.ssh/known_hosts",
            sessionConfig = mapOf("StrictHostKeyChecking" to "no"),
            openSSHConfig = "~/.ssh/config")
    val oschoices = listOf("linux", "darwin", "aix", "sunos")

    fun <T> howLongFor(what: () -> T): Pair<Long, T> {
        val begin = System.currentTimeMillis()
        val result = what()
        val end = System.currentTimeMillis()
        return Pair(end - begin, result)
    }
}
