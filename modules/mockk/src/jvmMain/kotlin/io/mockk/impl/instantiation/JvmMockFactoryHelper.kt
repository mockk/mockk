package io.mockk.impl.instantiation

import io.mockk.*
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.Stub
import io.mockk.core.ValueClassSupport.boxedClass
import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.Callable
import kotlin.coroutines.Continuation
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

object JvmMockFactoryHelper {
    fun mockHandler(stub: Stub) = object : MockKInvocationHandler {
        override fun invocation(self: Any, method: Method?, originalCall: Callable<*>?, args: Array<Any?>) =
            stdFunctions(self, method!!, args) {

                stub.handleInvocation(
                    self,
                    method.toDescription(),
                    {
                        handleOriginalCall(originalCall, method)
                    },
                    args,
                    findBackingField(self, method)
                )
            }
    }

    private fun findBackingField(clazz: KClass<*>, method: Method): KProperty1<*, *>? = runCatching {
        /**
         * `runCatching()` is used to avoid crashes when analyzing unsupported types as
         * top-file extension function resulting into
         * `Packages and file facades are not yet supported in Kotlin reflection.`
         *
         * Also, the functional types are unsupported, we skip them early.
         */

        if (Function::class.java.isAssignableFrom(clazz.java)) {
            return null
        }

        return clazz.memberProperties.firstOrNull {
            it.getter.javaMethod == method ||
                    (it is KMutableProperty<*> && it.setter.javaMethod == method)
        }
    }.getOrNull()

    private fun findBackingField(self: Any, method: Method): BackingFieldValueProvider {
        return {
            val property = findBackingField(self::class, method)
            property?.javaField?.let { field ->
                BackingFieldValue(
                    property.name,
                    {
                        InternalPlatformDsl.makeAccessible(field)
                        field.get(self)
                    },
                    {
                        InternalPlatformDsl.makeAccessible(field)
                        field.set(self, it)
                    }
                )
            }
        }
    }

    private inline fun stdFunctions(
        self: Any,
        method: Method,
        args: Array<Any?>,
        otherwise: () -> Any?
    ): Any? {
        if (self is Class<*>) {
            if (method.isHashCode()) {
                return System.identityHashCode(self)
            } else if (method.isEquals()) {
                return self === args[0]
            }
        }
        return otherwise()
    }

    private fun handleOriginalCall(originalMethod: Callable<*>?, method: Method): Any? {
        if (originalMethod == null) {
            throw MockKException("No way to call original method ${method.toDescription()}")
        }

        return try {
            originalMethod.call()
        } catch (ex: InvocationTargetException) {
            throw ex.cause ?: throw ex
        }
    }

    internal fun Method.toDescription(): MethodDescription {
        val cached = cache[this]
        if (cached != null) return cached

        val kotlinFunc =
            try {
                // workaround for
                //  https://github.com/mockk/mockk/issues/18
                //  https://github.com/mockk/mockk/issues/22
                kotlinFunction
            } catch (ex: Throwable) {
                null
            }

        val vararg = when {
            kotlinFunc != null ->
                kotlinFunc.parameters
                    .filter { it.kind != KParameter.Kind.INSTANCE }
                    .indexOfFirst { it.isVararg }

            isVarArgs ->
                parameterTypes.size - 1

            else -> -1
        }

        val returnTypeIsUnit = when {
            kotlinFunc != null ->
                kotlinFunc.returnType.toString() == "kotlin.Unit"

            else ->
                returnType == Void.TYPE
        }

        val returnTypeIsNothing =
            kotlinFunc?.returnType?.toString() == "kotlin.Nothing"

        val isSuspend = when {
            kotlinFunc != null ->
                kotlinFunc.isSuspend

            else -> parameterTypes.lastOrNull()?.let {
                Continuation::class.java.isAssignableFrom(it)
            } ?: false
        }

        val isFnCall = Function::class.java.isAssignableFrom(declaringClass)

        val kotlinReturnType = kotlinFunc?.returnType
            ?: findBackingField(declaringClass.kotlin, this)?.returnType
        val returnType: KClass<*> = when (kotlinReturnType) {
            is KType -> kotlinReturnType.classifier as? KClass<*> ?: returnType.kotlin
            is KClass<*> -> kotlinReturnType
            else -> returnType.kotlin
        }.boxedClass

        val androidCompatibleReturnType = if (returnType.qualifiedName in androidUnsupportedTypes) {
            this@toDescription.returnType.kotlin
        } else {
            returnType
        }
        val returnTypeNullable = kotlinReturnType?.isMarkedNullable ?: false

        val result = MethodDescription(
            name,
            androidCompatibleReturnType,
            returnTypeNullable,
            returnTypeIsUnit,
            returnTypeIsNothing,
            isSuspend,
            isFnCall,
            declaringClass.kotlin,
            parameterTypes.map { it.kotlin },
            vararg,
            Modifier.isPrivate(modifiers) ||
                    Modifier.isProtected(modifiers)
        )

        cache[this] = result

        return result
    }

    /**
     * These types have to be resolved to kotlin.Array on Android to work properly.
     */
    private val androidUnsupportedTypes = setOf(
        "kotlin.BooleanArray",
        "kotlin.ByteArray",
        "kotlin.ShortArray",
        "kotlin.CharArray",
        "kotlin.IntArray",
        "kotlin.LongArray",
        "kotlin.FloatArray",
        "kotlin.DoubleArray"
    )

    fun Method.isHashCode() = name == "hashCode" && parameterTypes.isEmpty()
    fun Method.isEquals() = name == "equals" && parameterTypes.size == 1 && parameterTypes[0] === Object::class.java

    val cache = InternalPlatform.weakMap<Method, MethodDescription>()


}
