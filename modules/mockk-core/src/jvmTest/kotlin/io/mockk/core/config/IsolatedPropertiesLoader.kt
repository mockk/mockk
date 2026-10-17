package io.mockk.core.config

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gives each test its own singleton without resetting process-wide settings. Only the
 * implementation is child-loaded, so tests can call the parent-loaded public interface.
 * Streams can be controlled for failure/concurrency tests or read from a real directory.
 */
internal class IsolatedPropertiesLoader(
    private val resourceDirectory: Path? = null,
) : URLClassLoader(
        listOfNotNull(
            resourceDirectory?.toUri()?.toURL(),
            UnifiedPropertiesLoader::class.java.protectionDomain.codeSource.location,
        ).toTypedArray(),
        PropertiesLoader::class.java.classLoader,
    ) {
    var unifiedContents: String? = null

    var legacyContents: String? = null

    var unifiedStreamFactory: (() -> InputStream?)? = null

    private val reads = AtomicInteger()
    val resourceReads: Int get() = reads.get()

    val propertiesLoader: PropertiesLoader by lazy {
        loadClass(UnifiedPropertiesLoader::class.java.name).getField("INSTANCE").get(null) as PropertiesLoader
    }

    override fun loadClass(
        name: String,
        resolve: Boolean,
    ): Class<*> {
        val target = UnifiedPropertiesLoader::class.java.name
        if (name != target && !name.startsWith("$target$")) return super.loadClass(name, resolve)
        return synchronized(this) {
            (findLoadedClass(name) ?: findClass(name)).also { if (resolve) resolveClass(it) }
        }
    }

    override fun getResourceAsStream(name: String): InputStream? {
        val factory: (() -> InputStream?)?
        val contents: String?
        when (name) {
            "mockk.properties" -> {
                factory = unifiedStreamFactory
                contents = unifiedContents
            }
            "io/mockk/settings.properties" -> {
                factory = null
                contents = legacyContents
            }
            else -> return super.getResourceAsStream(name)
        }
        reads.incrementAndGet()
        // Do not inherit configuration resources from the test runner's classpath.
        if (resourceDirectory != null) return findResource(name)?.openStream()
        return if (factory != null) {
            factory()
        } else {
            contents?.let {
                ByteArrayInputStream(it.toByteArray(Charsets.ISO_8859_1))
            }
        }
    }
}
