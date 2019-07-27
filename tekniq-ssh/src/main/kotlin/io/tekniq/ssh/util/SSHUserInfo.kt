package io.tekniq.ssh.util

import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo

data class SSHUserInfo(val sshPassword: String? = null, val sshPassphrase: String? = null) : UserInfo, UIKeyboardInteractive {

    override fun promptPassphrase(message: String?): Boolean = true
    override fun getPassphrase(): String = sshPassphrase.orEmpty()
    override fun getPassword(): String = sshPassword.orEmpty()
    override fun promptYesNo(message: String?): Boolean = true
    override fun showMessage(message: String?) {}
    override fun promptPassword(message: String?): Boolean = true

    override fun promptKeyboardInteractive(destination: String?, name: String?,
                                           instruction: String?, prompt: Array<out String>?,
                                           echo: BooleanArray?): Array<String> = arrayOf(password)

}
