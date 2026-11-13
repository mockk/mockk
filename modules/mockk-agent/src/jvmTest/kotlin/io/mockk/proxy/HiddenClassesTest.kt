package io.mockk.proxy

import io.mockk.proxy.jvm.HiddenClasses
import net.bytebuddy.ByteBuddy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method

class HiddenClassesTest {
    private val isHiddenSupported: Boolean =
        runCatching { Class::class.java.getMethod("isHidden") }.isSuccess

    @Test
    fun `regular classes are not hidden`() {
        assertFalse(HiddenClasses.isHidden(String::class.java))
        assertFalse(HiddenClasses.isHidden(HiddenClassesTest::class.java))
    }

    @Test
    fun `a hidden class defined through a lookup is detected`() {
        if (!isHiddenSupported) {
            // JDK 8-14 have no hidden classes; the `$$Lambda$` name check covers their lambda classes.
            return
        }

        val hiddenClass = defineHiddenProbe()

        assertTrue(HiddenClasses.isHidden(hiddenClass))
    }

    /**
     * `Lookup.defineHiddenClass`/`hiddenClass` are JDK 15+ APIs, resolved reflectively so this
     * test file stays loadable on the same JDKs [HiddenClasses] supports.
     */
    private fun defineHiddenProbe(): Class<*> {
        val bytes =
            ByteBuddy()
                .subclass(Any::class.java)
                .name("io.mockk.proxy.HiddenProbe")
                .make()
                .bytes

        val lookup = MethodHandles.lookup()
        val classOptions =
            java.lang.reflect.Array
                .newInstance(classOptionClass, 0)

        val defineHiddenClass: Method =
            MethodHandles.Lookup::class.java.getMethod(
                "defineHiddenClass",
                ByteArray::class.java,
                Boolean::class.javaPrimitiveType,
                classOptions::class.java,
            )

        val hiddenLookup = defineHiddenClass.invoke(lookup, bytes, true, classOptions) as MethodHandles.Lookup

        // The returned lookup's class is the hidden class itself.
        return hiddenLookup.lookupClass()
    }

    private companion object {
        val classOptionClass: Class<*> =
            Class.forName("java.lang.invoke.MethodHandles\$Lookup\$ClassOption")
    }
}
