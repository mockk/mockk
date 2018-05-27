package io.mockk.proxy.android

import android.os.Build
import io.mockk.proxy.*
import io.mockk.proxy.android.advice.Advice
import io.mockk.proxy.android.transformation.AndroidInlineInstrumentation
import io.mockk.proxy.android.transformation.AndroidSubclassInstrumentation
import io.mockk.proxy.android.transformation.InliningClassTransformer
import io.mockk.proxy.common.ProxyMaker
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import java.io.IOException

@Suppress("unused") // dynamically loaded
class AndroidMockKAgentFactory : MockKAgentFactory {
    private val handlers = AndroidMockKMap()
    private val staticHandlers = AndroidMockKMap()
    private val constructorHandlers = AndroidMockKMap()
    private val advice = Advice(handlers, staticHandlers, constructorHandlers)
    private val specMap = ClassTransformationSpecMap()

    lateinit var log: MockKAgentLogger

    override lateinit var instantiator: MockKInstantiatior
    override lateinit var proxyMaker: MockKProxyMaker
    override lateinit var staticProxyMaker: MockKStaticProxyMaker
    override lateinit var constructorProxyMaker: MockKConstructorProxyMaker

    override fun init(logFactory: MockKAgentLogFactory) {
        log = logFactory.logger(AndroidMockKAgentFactory::class.java)

        var inliner: AndroidInlineInstrumentation? = null
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
            val classTransformer = InliningClassTransformer(specMap)

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

            inliner = AndroidInlineInstrumentation(
                logFactory.logger(AndroidInlineInstrumentation::class.java),
                specMap,
                agent
            )

        } else {
            log.debug("Detected version prior to Android P. Not using inlining class transformer. Only proxy subclassing is available")
        }


        instantiator = OnjenesisInstantiator(
            logFactory.logger(OnjenesisInstantiator::class.java)
        )

        val subclasser = AndroidSubclassInstrumentation()

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
            staticHandlers
        )

        constructorProxyMaker = ConstructorProxyMaker(
            inliner,
            constructorHandlers
        )
    }

    companion object {
        private val dispatcherClassName = "io.mockk.proxy.android.AndroidMockKDispatcher"
        private val dispatcherJar = "dispatcher.jar"
    }
}
