package io.mockk.proxy.advice

import io.mockk.proxy.jvm.advice.JdkInternalCallGuard
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.ArrayList
import java.util.function.Consumer

class JdkInternalCallGuardTest {
    @Test
    fun `call from test code is not a JDK internal call`() {
        val probe = JdkCallProbe()
        assertFalse(probe.probeDirectly())
    }

    @Test
    fun `unknown intercepted method never reports a JDK internal call`() {
        val probe = JdkCallProbe()
        assertFalse(probe.probeWithForeignMethod())
    }

    @Test
    fun `call made by JDK code is a JDK internal call`() {
        val probe = JdkCallProbe()
        // ArrayList.forEach invokes the consumer from inside the JDK, which is exactly the
        // situation the guard must detect (ZipFile reading a jar through Files.readAttributes).
        val list = ArrayList(listOf(probe))
        list.forEach(probe)
        assertTrue(probe.probedThroughJdk)
    }
}

class JdkCallProbe : Consumer<JdkCallProbe> {
    var probedThroughJdk = false
        private set

    fun probeDirectly(): Boolean = JdkInternalCallGuard.isJdkInternalCall(JdkCallProbe::class.java, method("probeDirectly"))

    fun probeWithForeignMethod(): Boolean = JdkInternalCallGuard.isJdkInternalCall(String::class.java, method("probeDirectly"))

    override fun accept(t: JdkCallProbe) {
        probedThroughJdk = JdkInternalCallGuard.isJdkInternalCall(JdkCallProbe::class.java, method("accept"))
    }

    private fun method(name: String): java.lang.reflect.Method = JdkCallProbe::class.java.declaredMethods.first { it.name == name }
}
