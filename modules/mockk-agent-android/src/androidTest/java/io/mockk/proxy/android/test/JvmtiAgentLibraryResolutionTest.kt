package io.mockk.proxy.android.test

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import dalvik.system.BaseDexClassLoader
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies that libmockkjvmtiagent.so is resolvable via the classloader regardless of
 * whether the APK uses useLegacyPackaging=true (extracted to filesystem) or
 * useLegacyPackaging=false (stored uncompressed in APK, AGP 8.5+ default).
 *
 * Regression test for https://github.com/mockk/mockk/issues/1273.
 */
@RunWith(AndroidJUnit4::class)
class JvmtiAgentLibraryResolutionTest {
    @Rule
    @JvmField
    val minSdkRule = MinSdkRule()

    @Test
    @MinSdk(Build.VERSION_CODES.P)
    fun findLibraryReturnsResolvablePathForMockkJvmtiAgent() {
        val cl =
            javaClass.classLoader as? BaseDexClassLoader
                ?: error("ClassLoader is not a BaseDexClassLoader")

        val libPath = cl.findLibrary("mockkjvmtiagent")

        assertNotNull(
            "libmockkjvmtiagent.so was not found by the classloader. " +
                "This likely means the native library is missing from the test APK. " +
                "Check that mockk-agent-android is a dependency and that the ABI matches.",
            libPath,
        )

        // The path is either a plain filesystem path (useLegacyPackaging=true, lib extracted)
        // or an "apk!/entry" zip-entry path (useLegacyPackaging=false, AGP 8.5+ default).
        // In both cases the APK file before "!/" (or the path itself) must exist on disk.
        val apkOrFilePath = libPath!!.substringBefore("!/")
        assertTrue(
            "Expected APK or library file to exist on disk: $apkOrFilePath",
            File(apkOrFilePath).exists(),
        )
    }
}
