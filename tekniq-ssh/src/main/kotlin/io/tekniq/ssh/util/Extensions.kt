package io.tekniq.ssh.util

import java.io.BufferedInputStream
import java.nio.charset.Charset

fun BufferedInputStream.readUntil(predicate: (Int) -> Boolean): List<Int> {
    val bytes = mutableListOf<Int>()

    var b = this.read()
    while (b != -1 && !predicate(b)) {
        bytes.add(b)
        b = this.read()
    }
    return bytes
}

fun BufferedInputStream.readUntil(buffer: ByteArray, predicate: (Int) -> Boolean): List<Int> {
    val lengths = mutableListOf<Int>()

    var rval = this.read(buffer)
    while (rval != -1 && !predicate(rval)) {
        lengths.add(rval)
        rval = this.read(buffer)
    }
    return lengths
}

fun String.toCharset(): Charset {
    return Charset.forName(this)
}
