package io.mockk.proxy.jvm

import io.mockk.proxy.*
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.jvm.dispatcher.BootJarLoader
import io.mockk.proxy.jvm.dispatcher.JvmMockKWeakMap
import io.mockk.proxy.jvm.transformation.InlineInstrumentation
import io.mockk.proxy.jvm.transformation.InliningClassTransformer
import io.mockk.proxy.jvm.transformation.SubclassInstrumentation
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.dynamic.scaffold.TypeValidation
import java.lang.instrument.Instrumentation
import java.util.*

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

        val jvmInstrumenatation = initInstrumentation(loader)

        class Initializer {
            fun init() {
                val byteBuddy = ByteBuddy()
                    .with(TypeValidation.DISABLED)

                jvmInstantiator = ObjenesisInstantiator(
                    logFactory.logger(ObjenesisInstantiator::class.java),
                    byteBuddy
                )

                val handlers = handlerMap(jvmInstrumenatation != null)
                val staticHandlers = handlerMap(jvmInstrumenatation != null)
                val constructorHandlers = handlerMap(jvmInstrumenatation != null)

                val specMap = ClassTransformationSpecMap()


                val inliner = jvmInstrumenatation?.let {

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

                    InlineInstrumentation(
                        logFactory.logger(InlineInstrumentation::class.java),
                        specMap,
                        jvmInstrumenatation
                    )
                }

                val subclasser = SubclassInstrumentation(handlers, byteBuddy)


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

            private fun handlerMap(hasInstrumentation: Boolean) =
                if (hasInstrumentation)
                    JvmMockKWeakMap<Any, MockKInvocationHandler>()
                else
                    Collections.synchronizedMap(mutableMapOf<Any, MockKInvocationHandler>())
        }
        Initializer().init()
    }

    private fun initInstrumentation(loader: BootJarLoader): Instrumentation? {
        val instrumentation = ByteBuddyAgent.install()

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
