package io.tekniq.ssh.util

sealed class OS

class Linux : OS()
class AIX : OS()
class Darwin : OS()
class SunOS : OS()
class UnknownOS : OS()
