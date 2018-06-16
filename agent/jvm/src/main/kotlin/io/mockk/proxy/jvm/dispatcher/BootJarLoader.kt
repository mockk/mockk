package io.mockk.proxy.jvm.dispatcher

import io.mockk.proxy.MockKAgentLogger

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.instrument.Instrumentation
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

internal class BootJarLoader(
    private val log: MockKAgentLogger
) {
    fun loadBootJar(instrumentation: Instrumentation): Boolean {
        val bootJar = buildBootJar() ?: return false

        try {
            instrumentation.appendToBootstrapClassLoaderSearch(JarFile(bootJar))
        } catch (e: IOException) {
            log.trace(e, "Can't add to bootstrap classpath")
            return false
        }

        val classLoader = ClassLoader.getSystemClassLoader().root()

        for (name in classNames) {
            val cls = try {
                classLoader.loadClass(name)
            } catch (cnfe: ClassNotFoundException) {
                log.trace(cnfe, "Can't load class $name")
                return false
            }

            if (cls.classLoader != null) {
                log.trace("Classloader is not bootstrap for $name")
                return false
            }

            log.trace("Bootstrap class loaded " + cls.name)
        }

        return true
    }


    private tailrec fun ClassLoader.root(): ClassLoader =
        if (parent == null) this else parent.root()

    private fun buildBootJar(): File? {
        try {
            val boot = File.createTempFile("mockk_boot", ".jar")
            boot.deleteOnExit()

            val out = JarOutputStream(FileOutputStream(boot))
            try {
                for (name in classNames) {
                    if (!addClass(out, name)) {
                        return null
                    }
                }
            } finally {
                out.close()
            }
            return boot
        } catch (ex: IOException) {
            log.trace(ex, "Error creating boot jar")
            return null
        }

    }

    @Throws(IOException::class)
    private fun addClass(out: JarOutputStream, source: String): Boolean {
        val fileName = source.replace('.', '/')

        val classLoader = BootJarLoader::class.java.classLoader

        val inputStream: InputStream? = classLoader.getResourceAsStream("$fileName.clazz")
                ?: classLoader.getResourceAsStream("$fileName.class")

        if (inputStream == null) {
            log.trace("$fileName not found")
            return false
        }

        out.putNextEntry(JarEntry("$fileName.class"))
        inputStream.use { it.copyTo(out) }
        out.closeEntry()
        return true
    }

    companion object {
        private val pkg = "${BootJarLoader::class.java.`package`.name}."

        private val classNames = arrayOf(
            pkg + "JvmMockKDispatcher",
            pkg + "JvmMockKWeakMap",
            pkg + "JvmMockKWeakMap\$StrongKey",
            pkg + "JvmMockKWeakMap\$WeakKey"
        )
    }
}
