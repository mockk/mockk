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
import java.util.zip.ZipFile

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

    // Debug.attachJvmtiAgent rejects any path containing '=' (reserved as the lib=options
    // separator) with IllegalArgumentException. Two real-world cases produce such paths:
    //   1. AGP 8.5+ test APK default (useLegacyPackaging=false): the .so stays inside the APK
    //      as a zip entry, so findLibrary returns "<apk>!/lib/<abi>/libmockkjvmtiagent.so".
    //   2. Android 10+ install directories: paths embed base64-padded random tokens, e.g.
    //      "/data/app/~~xxx==/pkg-yyy==/lib/<abi>/libmockkjvmtiagent.so".
    // For (1) we extract the .so to the app's cache dir, whose path never contains '='. For
    // (2) the .so is already on the filesystem and reachable via the classloader's library
    // search path, so we fall back to the bare lib name and let the linker resolve it.
    private fun resolveAgentPath(cl: BaseDexClassLoader): String {
        val libPath = cl.findLibrary("mockkjvmtiagent") ?: return LIB_NAME
        if ("!/" in libPath) return extractAgentLibrary(libPath)
        if ('=' in libPath) return LIB_NAME
        return libPath
    }

    private fun extractAgentLibrary(zipEntryPath: String): String {
        val splitAt = zipEntryPath.indexOf("!/")
        val apkPath = zipEntryPath.substring(0, splitAt)
        val entryName = zipEntryPath.substring(splitAt + 2)
        val extracted = File.createTempFile("mockkjvmtiagent", ".so").apply { deleteOnExit() }
        ZipFile(apkPath).use { zip ->
            val entry =
                zip.getEntry(entryName)
                    ?: throw MockKAgentException("$entryName not found in $apkPath")
            zip.getInputStream(entry).use { input ->
                FileOutputStream(extracted).use { output -> input.copyTo(output) }
            }
        }
        return extracted.absolutePath
    }

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
