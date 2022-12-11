@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package io.mockk.proxy.android

import android.annotation.SuppressLint
import android.os.Build
import io.mockk.proxy.*
import io.mockk.proxy.android.advice.Advice
import io.mockk.proxy.android.transformation.AndroidInlineInstrumentation
import io.mockk.proxy.android.transformation.AndroidSubclassInstrumentation
import io.mockk.proxy.android.transformation.InliningClassTransformer
import io.mockk.proxy.common.ProxyMaker
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import java.io.IOException
import java.lang.reflect.Method

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

            // Set up exemption for blacklisted APIs to allow mocking on SDK objects with hidden methods.
            // https://android-developers.googleblog.com/2018/02/improving-stability-by-reducing-usage.html
            //
            // From API 30, this workaround no longer works.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                try {
                    val vmRuntimeClass = Class.forName(vmRuntimeClassName)
                    val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                            getDeclaredMethodMethodName,
                            String::class.java,
                            arrayOf<Class<*>>()::class.java
                    ) as Method
                    val getRuntime = getDeclaredMethod(
                            vmRuntimeClass,
                            getRuntimeMethodName,
                            null
                    ) as Method
                    val setHiddenApiExemptions = getDeclaredMethod(
                            vmRuntimeClass,
                            setHiddenApiExemptionsMethodName,
                            arrayOf(arrayOf<String>()::class.java)
                    ) as Method

                    setHiddenApiExemptions(getRuntime(null), arrayOf("L"))
                } catch (ex: Exception) {
                    throw MockKAgentException("Could not set up hiddenApiExemptions")
                }
            } else {
                try {
                    val vmDebugClass = Class.forName(vmDebugClassName)
                    @SuppressLint("DiscouragedPrivateApi")
                    val allowHiddenApiReflectionFrom = vmDebugClass.getDeclaredMethod(
                            allowHiddenApiReflectionFromMethodName,
                            Class::class.java
                    ) as Method

                    allowHiddenApiReflectionFrom(null, MethodDescriptor::class.java)
                } catch (e: Exception) {
                    throw MockKAgentException("Could not set up hiddenApiExemptions")
                }
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

        val subclasser = AndroidSubclassInstrumentation(
            inliner != null
        )

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
        private const val dispatcherClassName = "io.mockk.proxy.android.AndroidMockKDispatcher"
        private const val dispatcherJar = "dispatcher.jar"
        private const val vmRuntimeClassName = "dalvik.system.VMRuntime"
        private const val vmDebugClassName = "dalvik.system.VMDebug"
        private const val getDeclaredMethodMethodName = "getDeclaredMethod"
        private const val allowHiddenApiReflectionFromMethodName = "allowHiddenApiReflectionFrom"
        private const val getRuntimeMethodName = "getRuntime"
        private const val setHiddenApiExemptionsMethodName = "setHiddenApiExemptions"
    }
}
