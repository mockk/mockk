package io.mockk.impl.annotations

import io.mockk.MockKException
import io.mockk.MockKGateway
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class JvmMockInitializer(val gateway: MockKGateway) : MockKGateway.MockInitializer {
    override fun initAnnotatedMocks(targets: List<Any>) {
        for (target in targets) {
            initMock(target)
        }
    }

    @MockK
    fun initMock(target: Any) {
        val cls = target::class
        val injectMockProperties: MutableList<KProperty1<Any, Any>> = mutableListOf()

        for (property in cls.memberProperties) {
            property.annotated<MockK>(target) { annotation ->
                val type = property.returnType.classifier as? KClass<*> ?: return@annotated null

                gateway.mockFactory.mockk(
                        type,
                        overrideName(annotation.name, property.name),
                        false,
                        arrayOf()
                )

            }


            property.annotated<RelaxedMockK>(target) { annotation ->
                val type = property.returnType.classifier as? KClass<*> ?: return@annotated null

                gateway.mockFactory.mockk(
                        type,
                        overrideName(annotation.name, property.name),
                        true,
                        arrayOf()
                )

            }

            property.annotated<SpyK>(target) { annotation ->
                val obj = (property as KProperty1<Any, Any>).get(target)

                gateway.mockFactory.spyk(
                        null,
                        obj,
                        overrideName(annotation.name, property.name),
                        arrayOf()
                )
            }

            if (property.findAnnotation<InjectMockKs>() != null) {
                val injectMockProperty = property as KProperty1<Any, Any>
                injectMockProperties.add(injectMockProperty)
            }
        }

        for (property in injectMockProperties) {
            tryMakeAccessible(property)
            resolveInstance(target, property)
        }
    }

    private fun overrideName(annotationName: String, propertyName: String): String {
        return if (annotationName.isBlank()) {
            propertyName
        } else {
            annotationName
        }
    }

    private fun resolveInstance(target: Any, property: KProperty1<Any, Any>): Any? {

        var obj: Any?
        val objCls = property.returnType.classifier as? KClass<*>

        if (objCls != null) {
            obj = objCls.objectInstance

            if (obj == null) {
                if (objCls.constructors.isEmpty() && objCls.primaryConstructor == null) {
                    throw MockKException("No constructors found to instantiate @InjectMockK field ${property.name}")
                } else {

                    obj = resolveByConstructorInjection(target, objCls)

                    if (obj == null) {
                        try {
                            val noArgCtor = objCls.constructors.first {
                                it.parameters.isEmpty()
                            }

                            obj = noArgCtor.call()
                            resolveByPropertyInjection(target, obj)
                            (property as KMutableProperty1<Any, Any?>).set(target, obj)
                        } catch (ex: NoSuchElementException) {
                            throw MockKException("No suitable constructors found to instantiate @InjectMockK field ${property.name}")
                        }
                    } else {
                        (property as KMutableProperty1<Any, Any?>).set(target, obj)
                    }
                }
            } else {
                throw MockKException("We could not inject mocks inside of @InjectMockK field ${property.name} because " +
                        "the field already has a value.  Remove the field assignment, or provide your own dependencies.")
            }
        } else {
            throw MockKException("Unable to create @InjectMockK field ${property.name} - could not cast it into a KClass type " +
                    "required for instance creation.")
        }

        return obj
    }

    private fun resolveByConstructorInjection(target: Any, injectMockKCls: KClass<*>) : Any? {
        // use largest constructor like mockito - maybe we can improve this part
        val ctor = injectMockKCls.constructors.maxBy { it.parameters.size }
        val targetCls = target::class
        val noArg = ctor?.parameters?.isEmpty() ?: true

        if (!noArg) {
            try {
                val ctorParams = ctor?.parameters
                val matchingTypes: MutableMap<KParameter, Any?> = mutableMapOf()
                ctorParams?.forEach { ctorParam ->
                    targetCls.memberProperties.forEach { testProp ->
                        if (isMatchingType(ctorParam.type, testProp.returnType)) {
                            matchingTypes[ctorParam] = (testProp as KProperty1<Any, Any?>).get(target)
                        }
                    }
                }

                return ctor?.callBy(matchingTypes)
            } catch (ex: NoSuchMethodException) {
                // ignore, attempt property strategy
                ex.printStackTrace()
            }
        }

        return null
    }

    private fun resolveByPropertyInjection(target: Any, obj: Any) {
        val targetCls = target::class
        // probably optimize this for large tests
        for (objProperty in obj::class.memberProperties) {
            val matchingProperties = targetCls.memberProperties.filter { targetProperty ->
                isMatchingType(targetProperty.returnType, objProperty.returnType)
            }

            if (!matchingProperties.isEmpty()) {
                when (matchingProperties.size) {
                    1 -> {
                        val matchingProperty = (matchingProperties.first() as KProperty1<Any, Any>).get(target)
                        (objProperty as KMutableProperty1<Any, Any>).set(obj, matchingProperty)
                    }
                    else -> {
                            // todo: match by name like mockito for inject mock classes with same multiple types
                    }
                }
            } else {
                throw MockKException("No matching attributes found in class $target \n " +
                        "for @InjectMock field ${obj::class.qualifiedName}")
            }
        }
    }


    private fun isMatchingType(t1: KType, t2: KType) : Boolean {
        val targetPropertyType = t1.classifier as? KClass<*> ?: return false
        val objectPropertyType = t2.classifier as? KClass<*> ?: return false

        return targetPropertyType == objectPropertyType
    }

    private inline fun <reified T : Annotation> KProperty<*>.annotated(
            target: Any,
            block: (T) -> Any?
    ) {
        val annotation = findAnnotation<T>()
        if (annotation != null) {
            tryMakeAccessible(this)
            val ret = block(annotation)
            if (ret != null) {
                (this as KMutableProperty1<Any, Any>).set(target, ret)
            }
        }
    }

    private fun tryMakeAccessible(property: KProperty<*>) {
        try {
            property.isAccessible = true
        } catch (ex: Exception) {
            // skip
        }
    }
}