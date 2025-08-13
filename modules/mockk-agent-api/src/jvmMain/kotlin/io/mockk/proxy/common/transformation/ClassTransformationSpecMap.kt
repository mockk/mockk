package io.mockk.proxy.common.transformation

import io.mockk.proxy.common.transformation.TransformationType.*
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ClassTransformationSpecMap {
    private val classSpecs = WeakHashMap<Class<*>, ClassTransformationSpec>()
    private val transformationLock = ReentrantLock(true)
    private val specLock = ReentrantLock()

    fun applyTransformation(
        request: TransformationRequest,
        retransformClasses: (TransformationRequest) -> Unit
    ) = transformationLock.withLock {
        val result = mutableListOf<Class<*>>()

        specLock.withLock {
            for (cls in request.classes) {
                val spec = classSpecs[cls]
                    ?: ClassTransformationSpec(cls)

                val diff = if (request.untransform) -1 else 1

                val newSpec =
                    when (request.type) {
                        SIMPLE -> spec.copy(simpleIntercept = spec.simpleIntercept + diff)
                        STATIC -> spec.copy(staticIntercept = spec.staticIntercept + diff)
                        CONSTRUCTOR -> spec.copy(constructorIntercept = spec.constructorIntercept + diff)
                    }

                classSpecs[cls] = newSpec

                if (!(spec sameTransforms newSpec)) {
                    result.add(cls)
                }
            }
        }

        retransformClasses(request.copy(classes = result.toSet()))
    }

    fun shouldTransform(clazz: Class<*>?) =
        specLock.withLock {
            classSpecs[clazz] != null
        }

    operator fun get(clazz: Class<*>?) =
        specLock.withLock {
            classSpecs[clazz]
                ?.apply {
                    if (!shouldDoSomething) {
                        classSpecs.remove(clazz)
                    }
                }
        }

    fun transformationMap(request: TransformationRequest): Map<String, String> =
        specLock.withLock {
            request.classes.associate { it.simpleName to classSpecs[it].toString() }
        }

}
