package io.tekniq.ssh.util

import java.io.OutputStream

class Producer(val output: OutputStream) {
    private fun sendChar(char: Int) {
        output.write(char)
        output.flush()
    }

    private fun sendString(cmd: String) {
        output.write(cmd.toByteArray())
        nl()
        output.flush()
    }

    fun send(cmd: String) = sendString(cmd)
    fun write(str: String) {
        output.write(str.toByteArray())
        output.flush()
    }

    fun brk() = sendChar(3) //Ctrl-C
    fun eot() = sendChar(4) //Ctrl-D
    fun esc() = sendChar(27) //ESC
    fun nl() = sendChar(10) //LF or NEWLINE or ENTER or Ctrl-J
    fun cr() = sendChar(13) //CR

    fun close() = output.close()
}
