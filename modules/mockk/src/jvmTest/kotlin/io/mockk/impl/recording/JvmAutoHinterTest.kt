package io.mockk.impl.recording

import io.mockk.impl.recording.JvmAutoHinter.Companion.exceptionMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JvmAutoHinterTest {
    @Test
    fun extractsTargetClassFromHotSpotPreJep358Message() {
        val msg =
            "net.bytebuddy.renamed.java.lang.Object\$ByteBuddy\$abc " +
                "cannot be cast to java.lang.String"
        assertEquals(
            "java.lang.String",
            exceptionMessage
                .find(msg)
                ?.groups
                ?.get(3)
                ?.value,
        )
    }

    @Test
    fun extractsTargetClassFromHotSpotJep358Message() {
        val msg =
            "class net.bytebuddy.renamed.java.lang.Object\$ByteBuddy\$abc " +
                "cannot be cast to class java.lang.String " +
                "(net.bytebuddy.renamed.java.lang.Object\$ByteBuddy\$abc is in unnamed module " +
                "of loader net.bytebuddy.dynamic.loading.ByteArrayClassLoader @19569ebd; " +
                "java.lang.String is in module java.base of loader 'bootstrap')"
        assertEquals(
            "java.lang.String",
            exceptionMessage
                .find(msg)
                ?.groups
                ?.get(3)
                ?.value,
        )
    }

    @Test
    fun extractsTargetClassFromOpenJ9Message() {
        val msg =
            "net.bytebuddy.renamed.java.lang.Object\$ByteBuddy\$abc " +
                "incompatible with java.lang.String"
        assertEquals(
            "java.lang.String",
            exceptionMessage
                .find(msg)
                ?.groups
                ?.get(3)
                ?.value,
        )
    }

    @Test
    fun returnsNullOnUnrecognisedMessage() {
        assertNull(
            exceptionMessage
                .find("some other failure")
                ?.groups
                ?.get(3)
                ?.value,
        )
    }
}
