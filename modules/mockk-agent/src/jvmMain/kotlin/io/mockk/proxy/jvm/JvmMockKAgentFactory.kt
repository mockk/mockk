package io.mockk.proxy.jvm

import io.mockk.proxy.*
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.jvm.advice.jvm.MockHandlerMap
import io.mockk.proxy.jvm.dispatcher.BootJarLoader
import io.mockk.proxy.jvm.transformation.InliningClassTransformer
import io.mockk.proxy.jvm.transformation.JvmInlineInstrumentation
import io.mockk.proxy.jvm.transformation.SubclassInstrumentation
import net.bytebuddy.ByteBuddy
import net.bytebuddy.NamingStrategy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.scaffold.TypeValidation
import java.lang.instrument.Instrumentation
import java.util.concurrent.atomic.AtomicLong

class JvmMockKAgentFactory : MockKAgentFactory {
    private lateinit var log: MockKAgentLogger

    private lateinit var jvmInstantiator: ObjenesisInstantiator
    private lateinit var jvmProxyMaker: MockKProxyMaker
    private lateinit var jvmStaticProxyMaker: MockKStaticProxyMaker
    private lateinit var jvmConstructorProxyMaker: MockKConstructorProxyMaker

    override fun init(logFactory: MockKAgentLogFactory) {
        log = logFactory.logger(JvmMockKAgentFactory::class.java)

        val loader = BootJarLoader(
            logFactory.logger(BootJarLoader::class.java)
        )

        val jvmInstrumentation = initInstrumentation(loader)

        class Initializer {
            fun preload() {
                listOf(
                    "java.lang.WeakPairMap\$Pair\$Weak",
                    "java.lang.WeakPairMap\$Pair\$Lookup",
                    "java.lang.WeakPairMap",
                    "java.lang.WeakPairMap\$WeakRefPeer",
                    "java.lang.WeakPairMap\$Pair",
                    "java.lang.WeakPairMap\$Pair\$Weak\$1"
                ).forEach {
                    try {
                        Class.forName(it, false, null)
                    } catch (ignored: ClassNotFoundException) {
                         // skip
                    }
                }
            }

            fun init() {
                preload()

                val byteBuddy = ByteBuddy()
                    .with(TypeValidation.DISABLED)
                    .with(MockKSubclassNamingStrategy())


                jvmInstantiator = ObjenesisInstantiator(
                    logFactory.logger(ObjenesisInstantiator::class.java),
                    byteBuddy
                )

                val handlers = MockHandlerMap.create(jvmInstrumentation != null)
                val staticHandlers = MockHandlerMap.create(jvmInstrumentation != null)
                val constructorHandlers = MockHandlerMap.create(jvmInstrumentation != null)

                val specMap = ClassTransformationSpecMap()


                val inliner = jvmInstrumentation?.let {

                    it.addTransformer(
                        InliningClassTransformer(
                            logFactory.logger(InliningClassTransformer::class.java),
                            specMap,
                            handlers,
                            staticHandlers,
                            constructorHandlers,
                            byteBuddy
                        ),
                        true
                    )

                    JvmInlineInstrumentation(
                        logFactory.logger(JvmInlineInstrumentation::class.java),
                        specMap,
                        jvmInstrumentation
                    )
                }

                val subclasser = SubclassInstrumentation(
                    logFactory.logger(SubclassInstrumentation::class.java),
                    handlers,
                    byteBuddy
                )


                jvmProxyMaker = ProxyMaker(
                    logFactory.logger(ProxyMaker::class.java),
                    inliner,
                    subclasser,
                    jvmInstantiator,
                    handlers
                )

                jvmStaticProxyMaker = StaticProxyMaker(
                    logFactory.logger(StaticProxyMaker::class.java),
                    inliner,
                    staticHandlers
                )

                jvmConstructorProxyMaker = ConstructorProxyMaker(
                    logFactory.logger(ConstructorProxyMaker::class.java),
                    inliner,
                    constructorHandlers

                )
            }
        }
        Initializer().init()
    }

    private fun initInstrumentation(loader: BootJarLoader): Instrumentation? {
        val instrumentation: Instrumentation? = ByteBuddyAgent.install()

        if (instrumentation == null) {
            log.debug(
                "Can't install ByteBuddy agent.\n" +
                        "Try running VM with MockK Java Agent\n" +
                        "i.e. with -javaagent:mockk-agent.jar option."
            )
            return null
        }

        log.trace("Byte buddy agent installed")
        if (!loader.loadBootJar(instrumentation)) {
            log.trace("Can't inject boot jar.")
            return null
        }

        return instrumentation
    }

    override val instantiator get() = jvmInstantiator
    override val proxyMaker get() = jvmProxyMaker
    override val staticProxyMaker get() = jvmStaticProxyMaker
    override val constructorProxyMaker get() = jvmConstructorProxyMaker

}

internal class MockKSubclassNamingStrategy : NamingStrategy.AbstractBase() {
    val counter = AtomicLong()

    override fun name(superClass: TypeDescription): String {
        var baseName = superClass.name
        if (baseName.startsWith("java.")) {
            baseName = "io.mockk.renamed.$baseName"
        }
        return "$baseName\$Subclass${counter.getAndIncrement()}"
    }
}
