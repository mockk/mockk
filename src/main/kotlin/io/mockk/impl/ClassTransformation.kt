package io.mockk.impl

import io.mockk.MockKGateway
import io.mockk.external.logger
import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.Modifier
import javassist.bytecode.AccessFlag
import java.util.*


internal class MockKPoolHolder {
    companion object {
        val pool = TranslatingClassPool(MockKClassTranslator())
    }
}


internal class TranslatingClassPool(private val mockKClassTranslator: MockKClassTranslator)
    : ClassPool() {

    val log = logger<TranslatingClassPool>()

    init {
        appendSystemPath()
        mockKClassTranslator.start(this)
    }

    override fun get0(classname: String, useCache: Boolean): CtClass? {
        val cls = super.get0(classname, useCache)
        if (cls != null) {
            mockKClassTranslator.onLoad(cls)
        } else {
            log.debug { "Failed to load $classname class"}
        }
        return cls
    }
}

internal class MockKClassTranslator {
    lateinit var noArgsParamType: CtClass
    val log = logger<MockKClassTranslator>()

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
            clazz.classFile2.accessFlags = AccessFlag.of(Modifier.clear(modifiers, Modifier.FINAL))
        }
    }

}
