package io.mockk

import kotlinx.coroutines.runBlocking
import java.lang.reflect.AccessibleObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.reflect.*
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

actual object InternalPlatformDsl {

    actual fun identityHashCode(obj: Any): Int = System.identityHashCode(obj)

    actual fun <T> runCoroutine(block: suspend () -> T): T {
        return runBlocking {
            block()
        }
    }

    actual fun Any?.toStr() =
        try {
            when (this) {
                null -> "null"
                is BooleanArray -> this.contentToString()
                is ByteArray -> this.contentToString()
                is CharArray -> this.contentToString()
                is ShortArray -> this.contentToString()
                is IntArray -> this.contentToString()
                is LongArray -> this.contentToString()
                is FloatArray -> this.contentToString()
                is DoubleArray -> this.contentToString()
                is Array<*> -> this.contentDeepToString()
                Void.TYPE.kotlin -> "void"
                kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED -> "SUSPEND_MARKER"
                is Continuation<*> -> "continuation {}"
                is KClass<*> -> this.simpleName ?: "<null name class>"
                is Method -> name + "(" + parameterTypes.joinToString { it.simpleName } + ")"
                is Function<*> -> "lambda {}"
                else -> toString()
            }
        } catch (thr: Throwable) {
            "<error \"$thr\">"
        }

    actual fun deepEquals(obj1: Any?, obj2: Any?): Boolean {
        return when {
            obj1 === obj2 -> true

            obj1 == null || obj2 == null -> false

            obj1.javaClass.isArray && obj2.javaClass.isArray -> arrayDeepEquals(obj1, obj2)

            else -> obj1 == obj2
        }
    }

    private fun arrayDeepEquals(obj1: Any, obj2: Any): Boolean {
        return when (obj1) {
            is BooleanArray -> obj1 contentEquals obj2 as BooleanArray
            is ByteArray -> obj1 contentEquals obj2 as ByteArray
            is CharArray -> obj1 contentEquals obj2 as CharArray
            is ShortArray -> obj1 contentEquals obj2 as ShortArray
            is IntArray -> obj1 contentEquals obj2 as IntArray
            is LongArray -> obj1 contentEquals obj2 as LongArray
            is FloatArray -> obj1 contentEquals obj2 as FloatArray
            is DoubleArray -> obj1 contentEquals obj2 as DoubleArray
            else -> return obj1 as Array<*> contentDeepEquals obj2 as Array<*>
        }
    }

    actual fun unboxChar(value: Any): Any = value

    actual fun Any.toArray(): Array<*> =
        when (this) {
            is BooleanArray -> this.toTypedArray()
            is ByteArray -> this.toTypedArray()
            is CharArray -> this.toTypedArray()
            is ShortArray -> this.toTypedArray()
            is IntArray -> this.toTypedArray()
            is LongArray -> this.toTypedArray()
            is FloatArray -> this.toTypedArray()
            is DoubleArray -> this.toTypedArray()
            else -> this as Array<*>
        }

    actual fun classForName(name: String): Any = Class.forName(name).kotlin

    actual fun dynamicCall(
        self: Any,
        methodName: String,
        args: Array<out Any?>,
        anyContinuationGen: () -> Continuation<*>
    ): Any? {
        val params = arrayOf(self, *args)
        val func = self::class.allAncestorFunctions().firstOrNull {
            if (it.name != methodName) {
                return@firstOrNull false
            }
            if (it.parameters.size != params.size) {
                return@firstOrNull false
            }

            for ((idx, param) in it.parameters.withIndex()) {

                val matches = when (val classifier = param.type.classifier) {
                    is KClass<*> -> classifier.isInstance(params[idx])
                    is KTypeParameter -> classifier.upperBounds.anyIsInstance(params[idx])
                    else -> false
                }
                if (!matches) {
                    return@firstOrNull false
                }
            }

            return@firstOrNull true

        }
            ?: throw MockKException(
                "can't find function $methodName(${args.joinToString(", ")}) of class ${self.javaClass.name} for dynamic call.\n" +
                        "If you were trying to verify a private function, make sure to provide type information to exactly match the functions signature."
            )

        func.javaMethod?.let { makeAccessible(it) }
        return if (func.isSuspend) {
            func.call(*params, anyContinuationGen())
        } else {
            func.call(*params)
        }
    }

    private fun KClass<*>.allAncestorFunctions(): Sequence<KFunction<*>> {
        return (sequenceOf(this) + this.allSuperclasses.asSequence())
            .flatMap { it.functions }
    }

    private fun List<KType>.anyIsInstance(value: Any?): Boolean {
        return any { bound ->
            val classifier = bound.classifier
            if (classifier is KClass<*>) {
                classifier.isInstance(value)
            } else {
                false
            }
        }
    }

    actual fun dynamicGet(self: Any, name: String): Any? {
        val property = self::class.memberProperties
            .filterIsInstance<KProperty1<Any, Any?>>()
            .firstOrNull {
                it.name == name
            } ?: throw MockKException("can't find property $name for dynamic property get")

        property.isAccessible = true
        return property.get(self)
    }

    actual fun dynamicSet(self: Any, name: String, value: Any?) {
        val property = self::class.memberProperties
            .filterIsInstance<KMutableProperty1<Any, Any?>>()
            .firstOrNull {
                it.name == name
            } ?: throw MockKException("can't find property $name for dynamic property set")

        property.isAccessible = true
        return property.set(self, value)
    }

    actual fun dynamicSetField(self: Any, name: String, value: Any?) {
        val field = self.javaClass
            .declaredFields.firstOrNull { it.name == name }
            ?: return

        makeAccessible(field)
        field.set(self, value)
    }

    fun makeAccessible(obj: AccessibleObject) {
        try {
            obj.isAccessible = true
        } catch (ex: Throwable) {
            // skip
        }
    }

    actual fun <T> threadLocal(initializer: () -> T): InternalRef<T> {
        class TL : ThreadLocal<T>(), InternalRef<T> {
            override fun initialValue(): T {
                return initializer()
            }

            override val value: T
                get() = get()

        }
        return TL()
    }

    actual fun counter() = object : InternalCounter {
        val atomicValue = AtomicLong()

        override val value: Long
            get() = atomicValue.get()

        override fun increment() = atomicValue.getAndIncrement()
    }

    actual fun <T> coroutineCall(lambda: suspend () -> T): CoroutineCall<T> = JvmCoroutineCall(lambda)

    @Suppress("UNCHECKED_CAST")
    internal actual fun <T : Any> boxCast(
        cls: KClass<*>,
        arg: Any,
    ): T {
        return if (cls.isValue) {
            val constructor = cls.primaryConstructor!!.apply { isAccessible = true }
            constructor.call(arg) as T
        } else {
            arg as T
        }
    }
}

class JvmCoroutineCall<T>(private val lambda: suspend () -> T) : CoroutineCall<T> {
    companion object {
        val callMethod: Method = JvmCoroutineCall::class.java.getMethod("callCoroutine", Continuation::class.java)
    }

    suspend fun callCoroutine() = lambda()

    override fun callWithContinuation(continuation: Continuation<*>): T {
        return try {
            @Suppress("UNCHECKED_CAST")
            callMethod.invoke(this, continuation) as T
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }
}
