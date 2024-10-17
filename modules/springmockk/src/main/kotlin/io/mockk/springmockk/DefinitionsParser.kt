package io.mockk.springmockk

import org.springframework.core.ResolvableType
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy
import org.springframework.util.Assert
import org.springframework.util.ReflectionUtils
import org.springframework.util.StringUtils
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.TypeVariable
import java.util.*
import kotlin.reflect.KClass


/**
 * Parser to create {@link MockkDefinition} and {@link SpykDefinition} instances from
 * {@link MockkBean @MockkBean} and {@link SpykBean @SpykBean} annotations declared on or in a
 * class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author JB Nizet
 */
class DefinitionsParser(existing: Collection<Definition> = emptySet()) {
    private val definitions = LinkedHashSet<Definition>()
    private val definitionFields = mutableMapOf<Definition, Field>()

    init {
        definitions.addAll(existing)
    }

    val parsedDefinitions: Set<Definition>
        get() = Collections.unmodifiableSet(definitions)

    fun parse(source: Class<*>) {
        parseElement(source, null)
        ReflectionUtils.doWithFields(source) { element -> this.parseElement(element, source) }
    }

    private fun parseElement(element: AnnotatedElement, source: Class<*>?) {
        val annotations = MergedAnnotations.from(
            element,
            SearchStrategy.SUPERCLASS
        )
        annotations.stream(MockkBean::class.java)
            .map { it.synthesize() }
            .forEach {  parseMockkBeanAnnotation(it, element, source) }
        annotations.stream(SpykBean::class.java)
            .map { it.synthesize() }
            .forEach { parseSpykBeanAnnotation(it, element, source) }
    }

    private fun parseMockkBeanAnnotation(annotation: MockkBean, element: AnnotatedElement, source: Class<*>?) {
        val typesToMock = getOrDeduceTypes(element, annotation.value, source)
        check(!typesToMock.isEmpty()) { "Unable to deduce type to mock from $element" }
        if (StringUtils.hasLength(annotation.name)) {
            check(typesToMock.size == 1) { "The name attribute can only be used when mocking a single class" }
        }
        for (typeToMock in typesToMock) {
            val definition = MockkDefinition(
                name = if (annotation.name.isEmpty()) null else annotation.name,
                typeToMock = typeToMock,
                extraInterfaces = annotation.extraInterfaces,
                clear = annotation.clear,
                relaxed = annotation.relaxed,
                relaxUnitFun = annotation.relaxUnitFun,
                qualifier = QualifierDefinition.forElement(element)
            )
            addDefinition(element, definition, "mock")
        }
    }

    private fun parseSpykBeanAnnotation(annotation: SpykBean, element: AnnotatedElement, source: Class<*>?) {
        val typesToSpy = getOrDeduceTypes(element, annotation.value, source)
        Assert.state(
            !typesToSpy.isEmpty()
        ) { "Unable to deduce type to spy from $element" }
        if (StringUtils.hasLength(annotation.name)) {
            Assert.state(
                typesToSpy.size == 1,
                "The name attribute can only be used when spying a single class"
            )
        }
        for (typeToSpy in typesToSpy) {
            val definition = SpykDefinition(
                name = if (annotation.name.isEmpty()) null else annotation.name,
                typeToSpy = typeToSpy,
                clear = annotation.clear,
                qualifier = QualifierDefinition.forElement(element)
            )
            addDefinition(element, definition, "spy")
        }
    }

    private fun addDefinition(
        element: AnnotatedElement,
        definition: Definition,
        type: String
    ) {
        val isNewDefinition = this.definitions.add(definition)
        Assert.state(
            isNewDefinition
        ) { "Duplicate $type definition $definition" }
        if (element is Field) {
            this.definitionFields[definition] = element
        }
    }

    private fun getOrDeduceTypes(
        element: AnnotatedElement,
        value: Array<out KClass<*>>,
        source: Class<*>?
    ): Set<ResolvableType> {
        val types = LinkedHashSet<ResolvableType>()
        for (clazz in value) {
            types.add(ResolvableType.forClass(clazz.java))
        }
        if (types.isEmpty() && element is Field) {
            val field = element
            types.add(if (field.genericType is TypeVariable<*>) {
                ResolvableType.forField(field, source!!)
            } else {
                ResolvableType.forField(field)
            })
        }
        return types
    }

    fun getField(definition: Definition): Field? {
        return this.definitionFields[definition]
    }
}
