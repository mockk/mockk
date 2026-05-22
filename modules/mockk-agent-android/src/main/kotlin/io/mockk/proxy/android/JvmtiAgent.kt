/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UNUSED_PARAMETER")

package io.mockk.proxy.android

import android.os.Build
import android.os.Debug
import dalvik.system.BaseDexClassLoader
import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.android.transformation.InliningClassTransformer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.ProtectionDomain

internal class JvmtiAgent {
    var transformer: InliningClassTransformer? = null

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw MockKAgentException(
                "Requires API level " + Build.VERSION_CODES.P + ". API level is " +
                    Build.VERSION.SDK_INT,
            )
        }

        if (Build.VERSION.CODENAME != "P") {
        }

        val cl =
            JvmtiAgent::class.java.classLoader as? BaseDexClassLoader
                ?: throw MockKAgentException(
                    "Could not load jvmti plugin as AndroidMockKJvmtiAgent class was not loaded " + "by a BaseDexClassLoader",
                )

        Debug.attachJvmtiAgent(resolveAgentPath(cl), null, cl)
        nativeRegisterTransformerHook()
    }

    // AGP 8.5+ defaults to useLegacyPackaging=false for test APKs, so native libs are stored
    // uncompressed in the APK but not extracted to the filesystem. We use findLibrary to get
    // the canonical path: a plain filesystem path when extracted, or an "apk!/entry" zip-entry
    // path when not extracted. Android 6+ dlopen handles both forms natively, so we pass the
    // result directly to attachJvmtiAgent instead of relying on a bare-name dlopen search.
    private fun resolveAgentPath(cl: BaseDexClassLoader): String = cl.findLibrary("mockkjvmtiagent") ?: LIB_NAME

    fun appendToBootstrapClassLoaderSearch(inStream: InputStream) {
        val jarFile =
            File
                .createTempFile("mockk-boot", ".jar")
                .apply { deleteOnExit() }
                .also {
                    FileOutputStream(it).use { inStream.copyTo(it) }
                }

        nativeAppendToBootstrapClassLoaderSearch(jarFile.absolutePath)
    }

    fun requestTransformClasses(classes: Array<Class<*>>) {
        synchronized(lock) {
            nativeRetransformClasses(classes)
        }
    }

    @Suppress("unused") // called by JNI
    fun shouldTransform(classBeingRedefined: Class<*>?): Boolean {
        if (classBeingRedefined == null) {
            return false
        }
        return transformer?.shouldTransform(classBeingRedefined) ?: false
    }

    @Suppress("unused") // called by JNI
    fun runTransformers(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray,
    ) = transformer?.transform(classBeingRedefined, classfileBuffer) ?: classfileBuffer

    fun disconnect() {
        nativeUnregisterTransformerHook()
    }

    private external fun nativeRegisterTransformerHook()

    private external fun nativeUnregisterTransformerHook()

    private external fun nativeRetransformClasses(classes: Array<Class<*>>)

    companion object {
        private const val LIB_NAME = "libmockkjvmtiagent.so"
        private val lock = Any()

        @JvmStatic
        private external fun nativeAppendToBootstrapClassLoaderSearch(absolutePath: String)
    }
}
