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
        // Classes whose interception counts all returned to zero.
        val exhausted = mutableListOf<Class<*>>()

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

                classSpecs[cls] = newSpec
                if (!newSpec.shouldDoSomething) {
                    exhausted.add(cls)
                }

                if (!(spec sameTransforms newSpec)) {
                    result.add(cls)
                }
            }
        }

        try {
            retransformClasses(request.copy(classes = result.toSet()))
        } finally {
            // All interception counts are back to zero: drop the record so the registry does not
            // accumulate entries for classes that are no longer intercepted (issue #1289). A class
            // that can be unloaded would get rid of the entry anyway; JDK classes never unload,
            // so a lingering record would stay forever. The record is only dropped once the class
            // has been retransformed back: the transformers find it and hand the runtime an
            // uninstrumented version of the class, which is how the Android agent reverts a
            // class (#1630).
            specLock.withLock {
                for (cls in exhausted) {
                    // The transformers drop such a record themselves when they read it.
                    if (classSpecs[cls]?.shouldDoSomething == false) {
                        classSpecs.remove(cls)
                    }
                }
            }
        }
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
