package io.mockk.proxy.common.transformation

import io.mockk.proxy.common.transformation.TransformationType.*
import java.util.*

class ClassTransformationSpecMap {
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

                classSpecs[cls] = newSpec

                if (!(spec sameTransforms newSpec)) {
                    result.add(cls)
                }
            }

            request.copy(classes = result.toSet())
        }

    fun shouldTransform(clazz: Class<*>?) =
        synchronized(classSpecs) {
            classSpecs[clazz] != null
        }

    operator fun get(clazz: Class<*>?) =
        synchronized(classSpecs) {
            classSpecs[clazz]
                ?.apply {
                    if (!shouldDoSomething) {
                        classSpecs.remove(clazz)
                    }
                }
        }

    fun transformationMap(request: TransformationRequest): Map<String, String> =
        synchronized(classSpecs) {
            request.classes.map { it.simpleName to classSpecs[it].toString() }.toMap()
        }

}