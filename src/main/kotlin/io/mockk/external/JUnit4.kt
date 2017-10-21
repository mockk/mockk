package io.mockk.external

import io.mockk.impl.MockKClassTranslator
import io.mockk.impl.TranslatingClassPool
import javassist.Loader
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier

/**
 * Runner to transforms classes early with junit {@link org.junit.runner.RunWith}
 */
class MockKJUnitRunner(cls: Class<*>) : Runner() {

    private val pool = TranslatingClassPool(MockKClassTranslator())
    private val loader = Loader(pool)

    init {
        loader.delegateLoadingOf("jdk.internal.")
        loader.delegateLoadingOf("org.junit.runner.")
        Thread.currentThread().contextClassLoader = loader
    }

    private val parentRunner = ParentRunnerFinderDynamicFinder(cls) { loader.loadClass(it.name) }.runner

    override fun run(notifier: RunNotifier?) {
        parentRunner.run(notifier)
    }

    override fun getDescription(): Description = parentRunner.description
}


@PublishedApi
internal class ParentRunnerFinder(val cls: Class<*>) {
    val parentRunner = findParentRunWith()

    fun findParentRunWith(): Runner {
        var parent = cls.superclass

        while (parent != null) {
            val annotation = parent.getAnnotation(RunWith::class.java)
            if (annotation != null) {
                val constructor = annotation.value.java.getConstructor(Class::class.java)
                return constructor.newInstance(cls)
            }
            parent = parent.superclass
        }
        throw RuntimeException("not runner RunWith found")
    }
}

internal class ParentRunnerFinderDynamicFinder(cls: Class<*>, instrument: (Class<*>) -> Class<*>) {
    private val finderClass = instrument(ParentRunnerFinder::class.java)
    private val finderConstructor = finderClass.getConstructor(Class::class.java)
    private val getParentRunnerMethod = finderClass.getMethod("getParentRunner")
    val runner = getParentRunnerMethod.invoke(finderConstructor.newInstance(instrument(cls))) as Runner
}

