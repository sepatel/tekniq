package io.tekniq.ssh.util

data class Expect(val expectWhen: (String) -> Boolean, val send: String)
