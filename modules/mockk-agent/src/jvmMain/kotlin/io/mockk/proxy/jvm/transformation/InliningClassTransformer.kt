package io.mockk.proxy.jvm.transformation

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.jvm.advice.ProxyAdviceId
import io.mockk.proxy.jvm.advice.jvm.JvmMockKConstructorProxyAdvice
import io.mockk.proxy.jvm.advice.jvm.JvmMockKHashMapStaticProxyAdvice
import io.mockk.proxy.jvm.advice.jvm.JvmMockKProxyAdvice
import io.mockk.proxy.jvm.advice.jvm.JvmMockKStaticProxyAdvice
import io.mockk.proxy.jvm.advice.jvm.MockHandlerMap
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher
import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.ModifierReviewable.OfByteCodeElement
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.ClassFileLocator.Simple.of
import net.bytebuddy.matcher.ElementMatchers.*
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import java.util.concurrent.atomic.AtomicLong

internal class InliningClassTransformer(
    private val log: MockKAgentLogger,
    private val specMap: ClassTransformationSpecMap,
    private val handlers: MockHandlerMap,
    private val staticHandlers: MockHandlerMap,
    private val constructorHandlers: MockHandlerMap,
    private val byteBuddy: ByteBuddy
) : ClassFileTransformer {

    private val restrictedMethods = setOf(
        "java.lang.System.getSecurityManager"
    )

    private lateinit var advice: JvmMockKProxyAdvice
    private lateinit var staticAdvice: JvmMockKStaticProxyAdvice
    private lateinit var staticHashMapAdvice: JvmMockKHashMapStaticProxyAdvice
    private lateinit var constructorAdvice: JvmMockKConstructorProxyAdvice

    init {
        class AdviceBuilder {
            fun build() {
                advice = JvmMockKProxyAdvice(handlers)
                staticAdvice = JvmMockKStaticProxyAdvice(staticHandlers)
                staticHashMapAdvice = JvmMockKHashMapStaticProxyAdvice(staticHandlers)
                constructorAdvice = JvmMockKConstructorProxyAdvice(constructorHandlers)

                JvmMockKDispatcher.set(advice.id, advice)
                JvmMockKDispatcher.set(staticAdvice.id, staticAdvice)
                JvmMockKDispatcher.set(staticHashMapAdvice.id, staticHashMapAdvice)
                JvmMockKDispatcher.set(constructorAdvice.id, constructorAdvice)
            }
        }
        AdviceBuilder().build()
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray? {
        if (classBeingRedefined == null) {
            return classfileBuffer
        }

        val spec = specMap[classBeingRedefined]
                ?: return classfileBuffer

        try {
            val builder = byteBuddy.redefine(classBeingRedefined, of(classBeingRedefined.name, classfileBuffer))
                .visit(FixParameterNamesVisitor(classBeingRedefined))

            val type = builder
                .run { if (spec.shouldDoSimpleIntercept) visit(simpleAdvice()) else this }
                .run { if (spec.shouldDoStaticIntercept) visit(staticAdvice(className)) else this }
                .run { if (spec.shouldDoConstructorIntercept) visit(constructorAdvice()) else this }
                .make()

            try {
                val property = System.getProperty("io.mockk.classdump.path")
                if (property != null) {
                    val nextIndex = classDumpIndex.incrementAndGet().toString()
                    val storePath = File(File(property, "inline"), nextIndex)
                    type.saveIn(storePath)
                }
            } catch (ex: Exception) {
                log.trace(ex, "Failed to save file to a dump")
            }

            return type.bytes

        } catch (e: Throwable) {
            log.warn(e, "Failed to transform class $className")
            return null
        }
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun simpleAdvice() =
        Advice.withCustomMapping()
            .bind(ProxyAdviceId::class.java, advice.id)
            .to(JvmMockKProxyAdvice::class.java)
            .on(
                isMethod<MethodDescription>()
                    .and(not<OfByteCodeElement>(isStatic<OfByteCodeElement>()))
                    .and(not<MethodDescription>(isDefaultFinalizer<MethodDescription>()))
                    .and(not<MethodDescription>(this::matchValueClassGetValueMethod))
            )

    @Suppress("RemoveExplicitTypeArguments")
    private fun staticAdvice(className: String) =
        Advice.withCustomMapping()
            .bind(ProxyAdviceId::class.java, staticProxyAdviceId(className))
            .to(staticProxyAdvice(className))
            .on(
                isStatic<OfByteCodeElement>()
                    .and(not<MethodDescription>(isTypeInitializer<MethodDescription>()))
                    .and(not<MethodDescription>(isConstructor<MethodDescription>()))
                    .and(not<MethodDescription>(this::matchRestrictedMethods))
            )

    private fun matchRestrictedMethods(desc: MethodDescription) =
        desc.declaringType.typeName + "." + desc.name in restrictedMethods

    private fun matchValueClassGetValueMethod(desc: MethodDescription): Boolean {
        return isFinal<MethodDescription>()
            .and(isDeclaredBy(isAnnotatedWith(JvmInline::class.java)))
            .and(named("getValue"))
            .and(isPublic())
            .and(takesNoArguments())
            .matches(desc)
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun constructorAdvice(): AsmVisitorWrapper.ForDeclaredMethods {
        return Advice.withCustomMapping()
            .bind(ProxyAdviceId::class.java, constructorAdvice.id)
            .to(JvmMockKConstructorProxyAdvice::class.java)
            .on(isConstructor())
    }


    // workaround #35
    private fun staticProxyAdviceId(className: String) =
        when (className) {
            "java/util/HashMap" -> staticHashMapAdvice.id
            else -> staticAdvice.id
        }

    // workaround #35
    private fun staticProxyAdvice(className: String) =
        when (className) {
            "java/util/HashMap" -> JvmMockKHashMapStaticProxyAdvice::class.java
            else -> JvmMockKStaticProxyAdvice::class.java
        }

    companion object {
        val classDumpIndex = AtomicLong()
    }
}
