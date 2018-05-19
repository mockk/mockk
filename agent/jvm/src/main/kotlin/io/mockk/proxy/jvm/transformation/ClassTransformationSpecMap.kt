package io.mockk.proxy.jvm.transformation

import io.mockk.proxy.jvm.transformation.TransformationType.*
import java.util.*

internal class ClassTransformationSpecMap {
    private val classSpecs = WeakHashMap<Class<*>, ClassTransformationSpec>()

    fun applyTransformationRequest(request: TransformationRequest) =
        synchronized(classSpecs) {
            val result = mutableListOf<Class<*>>()

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

                if (newSpec.isEmpty()) {
                    classSpecs.remove(cls)
                } else {
                    classSpecs[cls] = newSpec
                }

                if (!(spec sameTransforms newSpec)) {
                    result.add(cls)
                }
            }

            request.copy(classes = result.toSet())
        }

    operator fun get(clazz: Class<*>?) =
        synchronized(classSpecs) {
            classSpecs[clazz]
        }

}