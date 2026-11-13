package io.mockk.impl.instantiation

import io.mockk.BackingFieldValue
import io.mockk.BackingFieldValueProvider
import io.mockk.InternalPlatformDsl
import io.mockk.MethodDescription
import io.mockk.MockKException
import io.mockk.core.ValueClassSupport.innermostBoxedClass
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.Stub
import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.Callable
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

object JvmMockFactoryHelper {
    fun mockHandler(stub: Stub) =
        object : MockKInvocationHandler {
            override fun invocation(
                self: Any,
                method: Method?,
                originalCall: Callable<*>?,
                args: Array<Any?>,
            ) = stdFunctions(self, method!!, args) {
                if (method.isKotlinInline()) {
                    throw MockKException("Mocking Kotlin inline functions is not supported")
                }

                if (method.isResumedBy(args)) {
                    return@stdFunctions handleOriginalCall(originalCall, method, stub)
                }

                stub.handleInvocation(
                    self,
                    method.toDescription(),
                    {
                        handleOriginalCall(originalCall, method, stub)
                    },
                    args,
                    findBackingField(self, method),
                )
            }
        }

    private fun findBackingField(
        clazz: KClass<*>,
        method: Method,
    ): KProperty1<*, *>? =
        runCatching {
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

    private fun findBackingField(
        self: Any,
        method: Method,
    ): BackingFieldValueProvider =
        {
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
                    },
                )
            }
        }

    private inline fun stdFunctions(
        self: Any,
        method: Method,
        args: Array<Any?>,
        otherwise: () -> Any?,
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

    private fun handleOriginalCall(
        originalMethod: Callable<*>?,
        method: Method,
        stub: Stub,
    ): Any? {
        if (originalMethod == null) {
            throw MockKException("No way to call original method ${method.toDescription()}")
        }

        return try {
            originalMethod.call()
        } catch (ex: InvocationTargetException) {
            val cause = ex.cause ?: throw ex

            throw if (cause.isAndroidNotMockedStubException()) {
                MockKException(androidNotMockedStubMessage(cause, method.toDescription(), stub), cause)
            } else {
                cause
            }
        }
    }

    /**
     * Unit tests on the JVM get their android.jar classes from the mockable jar the Android
     * Gradle Plugin generates: unless the build sets `returnDefaultValues = true`, every method
     * body there is replaced with `throw new RuntimeException("Method <name> in <class> not
     * mocked. See ...")`. Mocks that fall through to the original implementation (static mocks,
     * object mocks, spies) hit exactly that body when no answer matched the call, and the raw
     * exception reads as if the mocking setup was ignored altogether — although usually the
     * answers are there and it is just a sibling overload that carries them (#1250).
     *
     * The pattern is specific enough that real code throwing it is implausible; if that ever
     * happens, the original exception stays attached as the cause, so nothing is lost.
     */
    private fun Throwable.isAndroidNotMockedStubException(): Boolean {
        if (this !is RuntimeException) return false
        val text = message ?: return false
        return text.startsWith("Method ") && " not mocked" in text
    }

    private fun androidNotMockedStubMessage(
        cause: Throwable,
        description: MethodDescription,
        stub: Stub,
    ): String {
        val configured =
            stub
                .matcherUsages()
                .keys
                .map { "${it.method.declaringClass.simpleName}.${it.method}" }
                .distinct()
                .sorted()
                .joinToString("\n  ")
        val configuredSection =
            if (configured.isEmpty()) {
                "No answers are configured on the mock at all"
            } else {
                "Answers configured on the mock match only:\n  $configured"
            }
        val suggestedMatchers = description.paramTypes.joinToString(", ") { "any<${it.simpleName}>()" }
        val called = "${description.declaringClass.simpleName}.$description"
        val suggestedCall = "${description.declaringClass.simpleName}.${description.name}($suggestedMatchers)"

        return "no answer matched $called, so the call ran the original implementation, " +
            "which is an Android mockable-jar stub and threw:\n" +
            "  $cause\n" +
            "$configuredSection.\n" +
            "An every { ... } block answers only the exact overload signature it records, so " +
            "stub the overload that is actually called, e.g. " +
            "every { $suggestedCall } returns ..."
    }

    /**
     * A suspend function that has suspended is resumed by its own continuation calling the function
     * again (the compiler-generated continuation class declared in it, with the sign bit of `label`
     * set). That call continues the original invocation rather than making a new one, so it goes
     * straight to the original code without being recorded or answered (#554).
     */
    private fun Method.isResumedBy(args: Array<Any?>): Boolean {
        val continuation = args.lastOrNull() as? Continuation<*> ?: return false
        val continuationClass = continuation.javaClass
        if (continuationClass.enclosingMethod != this) return false

        val label =
            runCatching {
                val field = continuationClass.getDeclaredField("label")
                InternalPlatformDsl.makeAccessible(field)
                field.getInt(continuation)
            }.getOrNull() ?: return false
        return label and Int.MIN_VALUE != 0
    }

    private fun Method.isKotlinInline(): Boolean =
        try {
            kotlinFunction?.isInline == true
        } catch (_: Throwable) {
            // Reading kotlinFunction makes kotlin-reflect resolve the whole declaring class
            // hierarchy, which fails in unforeseeable ways on classes it cannot analyze (#1432,
            // #1439, #1512), so the same broad catch as toDescription() below is needed.
            // Detection is best-effort: an unanalyzable method counts as not inline and mocking
            // proceeds.
            false
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

        val vararg =
            when {
                kotlinFunc != null ->
                    kotlinFunc.parameters
                        .filter { it.kind != KParameter.Kind.INSTANCE }
                        .indexOfFirst { it.isVararg }

                isVarArgs ->
                    parameterTypes.size - 1

                else -> -1
            }

        val returnTypeIsUnit =
            when {
                kotlinFunc != null ->
                    kotlinFunc.returnType.toString() == "kotlin.Unit"

                else ->
                    returnType == Void.TYPE
            }

        val returnTypeIsNothing =
            kotlinFunc?.returnType?.toString() == "kotlin.Nothing"

        val lastParamIsContinuation =
            parameterTypes.lastOrNull()?.let {
                Continuation::class.java.isAssignableFrom(it)
            } ?: false

        // `kotlinFunc?.isSuspend` is normally authoritative, but it is not trusted on its own:
        // some kotlin-reflect versions misreport `isSuspend = false` for a suspend member of a
        // functional (SAM) interface, even though the compiled method still carries the trailing
        // `Continuation` parameter (see #1555). Falling back to the Java-level signature check
        // whenever it disagrees keeps suspend detection correct regardless of that skew.
        val isSuspend = kotlinFunc?.isSuspend == true || lastParamIsContinuation

        val isFnCall = Function::class.java.isAssignableFrom(declaringClass)

        val kotlinReturnType =
            kotlinFunc?.returnType
                ?: findBackingField(declaringClass.kotlin, this)?.returnType
        val returnTypeNullable = kotlinReturnType?.isMarkedNullable ?: false
        val returnTypeClass: KClass<*> =
            when (kotlinReturnType) {
                is KType -> kotlinReturnType.classifier as? KClass<*> ?: returnType.kotlin
                is KClass<*> -> kotlinReturnType
                else -> returnType.kotlin
            }
        // Use innermostBoxedClass for all value classes (nullable or not) to handle nested value classes
        // while preventing infinite recursion (issues #1103, #1308, #1475)
        val returnType: KClass<*> = returnTypeClass.innermostBoxedClass()

        val androidCompatibleReturnType =
            if (returnType.qualifiedName in androidUnsupportedTypes) {
                this@toDescription.returnType.kotlin
            } else {
                returnType
            }

        val result =
            MethodDescription(
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
                    Modifier.isProtected(modifiers),
            )

        cache[this] = result

        return result
    }

    /**
     * These types have to be resolved to kotlin.Array on Android to work properly.
     */
    private val androidUnsupportedTypes =
        setOf(
            "kotlin.BooleanArray",
            "kotlin.ByteArray",
            "kotlin.ShortArray",
            "kotlin.CharArray",
            "kotlin.IntArray",
            "kotlin.LongArray",
            "kotlin.FloatArray",
            "kotlin.DoubleArray",
        )

    fun Method.isHashCode() = name == "hashCode" && parameterTypes.isEmpty()

    fun Method.isEquals() = name == "equals" && parameterTypes.size == 1 && parameterTypes[0] === Object::class.java

    val cache = InternalPlatform.weakMap<Method, MethodDescription>()
}
