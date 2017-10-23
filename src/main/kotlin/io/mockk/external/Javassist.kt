package io.mockk.external

import io.mockk.MockKGateway
import javassist.*
import javassist.bytecode.AccessFlag
import java.util.*


internal class JavassistPoolHolder {
    companion object {
        val pool = JavassistTranslatingClassPool(JavassistTranslator())
    }
}

internal class JavassistClassLoader(val pool: ClassPool) : Loader(pool) {
    private val log = logger<JavassistClassLoader>()

    init {
        delegateLoadingOf("jdk.internal.")
        delegateLoadingOf("org.junit.runner.")
        delegateLoadingOf("sun.")
    }

    override fun loadClass(name: String): Class<*> {
        val cls = super.loadClass(name)
        log.info { "Loaded class $cls hashcode=${Integer.toHexString(cls.hashCode())}" }
        return cls
    }
}

internal class JavassistTranslatingClassPool(private val translator: JavassistTranslator)
    : ClassPool() {

    val log = logger<JavassistTranslatingClassPool>()

    init {
        appendSystemPath()
        translator.start(this)
    }

    override fun get0(classname: String, useCache: Boolean): CtClass? {
        val cls = super.get0(classname, useCache)
        if (cls != null) {
            translator.onLoad(cls)
        } else {
            log.debug { "Failed to load $classname class"}
        }
        return cls
    }
}

internal class JavassistTranslator {
    val log = logger<JavassistTranslator>()

    lateinit var noArgsParamType: CtClass

    val checkModifyMethod = CtClass::class.java.getDeclaredMethod("checkModify")
    init {
        checkModifyMethod.isAccessible = true
    }

    fun start(pool: ClassPool) {
        noArgsParamType = pool.makeClass(MockKGateway.NO_ARG_TYPE_NAME)
    }

    val load: MutableSet<String> = Collections.synchronizedSet(hashSetOf<String>())

    fun onLoad(cls: CtClass) {
        if (!load.add(cls.name) || cls.isFrozen) {
            return
        }
        log.trace { "Translating ${cls.name}" }
        removeFinal(cls)
//        addNoArgsConstructor(cls)
    }

    private fun addNoArgsConstructor(cls: CtClass) {
        if (cls.isAnnotation || cls.isArray || cls.isEnum || cls.isInterface) {
            return
        }

        if (cls.constructors.any { isNoArgsConstructor(it) }) {
            return
        }

        if (cls.superclass == null) {
            return
        }

        with(cls.superclass) {
            when {
                constructors.any { isNoArgsConstructor(it) } -> {
                    if (cls.constructors.any { isNoArgsConstructor(it) }) {
                        return@with
                    }

                    val newConstructor = CtConstructor(arrayOf(noArgsParamType), cls)
                    cls.addConstructor(newConstructor)
                    newConstructor.setBody("super($1);")
                }
                constructors.any { it.parameterTypes.isEmpty() } -> {
                    if (cls.constructors.any { isNoArgsConstructor(it) }) {
                        return@with
                    }

                    val newConstructor = CtConstructor(arrayOf(noArgsParamType), cls)
                    cls.addConstructor(newConstructor)
                    newConstructor.setBody("super();")
                }
            }
        }
    }

    private fun isNoArgsConstructor(it: CtConstructor) =
            it.parameterTypes.size == 1 && it.parameterTypes[0] == noArgsParamType

    fun removeFinal(clazz: CtClass) {
        removeFinalOnClass(clazz)
        removeFinalOnMethods(clazz)
        clazz.stopPruning(true)
    }

    private fun removeFinalOnMethods(clazz: CtClass) {
        clazz.declaredMethods.forEach {
            if (Modifier.isFinal(it.modifiers)) {
                it.modifiers = Modifier.clear(it.modifiers, Modifier.FINAL)
            }
        }
    }


    private fun removeFinalOnClass(clazz: CtClass) {
        val modifiers = clazz.modifiers
        if (Modifier.isFinal(modifiers)) {
            checkModifyMethod.invoke(clazz)
            clazz.classFile2.accessFlags = AccessFlag.of(Modifier.clear(modifiers, Modifier.FINAL))
        }
    }

}
