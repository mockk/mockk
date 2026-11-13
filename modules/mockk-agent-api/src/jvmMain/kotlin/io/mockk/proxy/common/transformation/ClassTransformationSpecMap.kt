package io.mockk.proxy.common.transformation

import io.mockk.proxy.common.transformation.TransformationType.CONSTRUCTOR
import io.mockk.proxy.common.transformation.TransformationType.SIMPLE
import io.mockk.proxy.common.transformation.TransformationType.STATIC
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ClassTransformationSpecMap {
    private val classSpecs = WeakHashMap<Class<*>, ClassTransformationSpec>()
    private val transformationLock = ReentrantLock(true)
    private val specLock = ReentrantLock()

    fun applyTransformation(
        request: TransformationRequest,
        retransformClasses: (TransformationRequest) -> Unit,
    ) = transformationLock.withLock {
        val result = mutableListOf<Class<*>>()

        specLock.withLock {
            for (cls in request.classes) {
                val spec =
                    classSpecs[cls]
                        ?: ClassTransformationSpec(cls)

                val diff = if (request.untransform) -1 else 1

                val newSpec =
                    when (request.type) {
                        SIMPLE -> spec.copy(simpleIntercept = spec.simpleIntercept + diff)
                        STATIC -> spec.copy(staticIntercept = spec.staticIntercept + diff)
                        CONSTRUCTOR -> spec.copy(constructorIntercept = spec.constructorIntercept + diff)
                    }

                if (newSpec.shouldDoSomething) {
                    classSpecs[cls] = newSpec
                } else {
                    // All interception counts are back to zero: drop the record so the
                    // registry does not accumulate entries for classes that are no
                    // longer intercepted (issue #1289). A class that can be unloaded
                    // would get rid of the entry anyway; JDK classes never unload,
                    // so a lingering record would stay forever.
                    classSpecs.remove(cls)
                }

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
