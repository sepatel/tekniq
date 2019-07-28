import io.tekniq.ssh.SSH
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class SSHTest {
    //JschLogger.init()

    @Ignore
    @Test
    fun simpleConnectivity() {
        println("Username is ${TestHelper.opts.username}")
        SSH.once(TestHelper.opts) {
            assertEquals("hello", execOnce("echo 'hello'"))
        }
    }
}

