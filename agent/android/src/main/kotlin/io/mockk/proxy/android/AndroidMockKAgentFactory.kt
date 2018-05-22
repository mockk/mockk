package io.mockk.proxy.android

import android.os.Build
import io.mockk.agent.*
import io.mockk.proxy.android.advice.Advice
import io.mockk.proxy.android.transformation.ClassTransformationSpecMap
import io.mockk.proxy.android.transformation.InlineInstrumentation
import io.mockk.proxy.android.transformation.InliningClassTransformer
import io.mockk.proxy.android.transformation.SubclassInstrumentation
import java.io.IOException

@Suppress("unused") // dynamically loaded
class AndroidMockKAgentFactory : MockKAgentFactory {
    private val handlers = AndroidMockKMap()
    private val advice = Advice(handlers)
    private val specMap = ClassTransformationSpecMap()

    lateinit var log: MockKAgentLogger

    private lateinit var instantiator: MockKInstantiatior
    private lateinit var proxyMaker: MockKProxyMaker
    private lateinit var staticProxyMaker: MockKStaticProxyMaker
    private lateinit var constructorProxyMaker: MockKConstructorProxyMaker

    override fun init(logFactory: MockKAgentLogFactory) {
        log = logFactory.logger(AndroidMockKAgentFactory::class.java)

        var inliner: InlineInstrumentation? = null
        if (Build.VERSION.CODENAME == "P") { // FIXME >= 'P'
            val agent: JvmtiAgent
            val dispatcherClass: Class<*>
            try {
                agent = JvmtiAgent()

                val dispatcherJar =
                    ProxyMaker::class
                        .java
                        .classLoader
                        .getResource(dispatcherJar)
                            ?: throw MockKAgentException("'$dispatcherJar' not found")

                dispatcherJar
                    .openStream()
                    .use { inStream ->
                        agent.appendToBootstrapClassLoaderSearch(inStream)
                    }

                dispatcherClass = Class.forName(
                    dispatcherClassName,
                    true,
                    Any::class.java.classLoader
                ) ?: throw MockKAgentException("$dispatcherClassName could not be loaded")
            } catch (cnfe: ClassNotFoundException) {
                throw MockKAgentException(
                    "MockK failed to inject the AndroidMockKDispatcher class into the "
                            + "bootstrap class loader\n\nIt seems like your current VM does not "
                            + "support the jvmti API correctly.", cnfe
                )
            } catch (ioe: IOException) {
                throw MockKAgentException(
                    "MockK could not self-attach a jvmti agent to the current VM. This "
                            + "feature is required for inline mocking.\nThis error occured due to an "
                            + "I/O error during the creation of this agent: " + ioe + "\n\n"
                            + "Potentially, the current VM does not support the jvmti API correctly", ioe
                )
            } catch (ex: MockKAgentException) {
                throw ex
            } catch (ex: Exception) {
                throw MockKAgentException(
                    "Could not initialize inline mock maker.\n"
                            + "\n"
                            + "Release: Android " + Build.VERSION.RELEASE + " " + Build.VERSION.INCREMENTAL
                            + "Device: " + Build.BRAND + " " + Build.MODEL, ex
                )
            }


            log.debug("Android P or higher detected. Using inlining class transformer")
            val classTransformer = InliningClassTransformer(agent, specMap)

            try {
                dispatcherClass.getMethod("set", String::class.java, Any::class.java)
                    .invoke(
                        null,
                        classTransformer.identifier,
                        advice
                    )
            } catch (e: Exception) {
                throw MockKAgentException("Failed to set advice in dispatcher", e)
            }

            agent.transformer = classTransformer

            inliner = InlineInstrumentation(
                logFactory.logger(InlineInstrumentation::class.java),
                specMap,
                agent
            )

        } else {
            log.debug("Detected version prior to Android P. Not using inlining class transformer. Only proxy subclassing is available")
        }


        instantiator = OnjenesisInstantiator(
            logFactory.logger(OnjenesisInstantiator::class.java)
        )

        val subclasser = SubclassInstrumentation()

        proxyMaker = ProxyMaker(
            logFactory.logger(
                ProxyMaker::class.java
            ),
            inliner,
            subclasser,
            instantiator,
            handlers
        )

        staticProxyMaker = StaticProxyMaker(
            inliner,
            handlers
        )
    }

    override fun getInstantiator() = instantiator
    override fun getProxyMaker() = proxyMaker
    override fun getStaticProxyMaker() = staticProxyMaker

    override fun getConstructorProxyMaker() = constructorProxyMaker

    companion object {
        private val dispatcherClassName = "io.mockk.proxy.android.AndroidMockKDispatcher"
        private val dispatcherJar = "dispatcher.jar"
    }
}
