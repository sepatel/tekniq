package io.tekniq.ssh.util

class SSHTimeoutException(val stdout: String, val stderr: String) : Exception("io.tekniq.ssh.SSH Timeout")
