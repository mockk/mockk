package com.ninjasquad.springmockk

import org.springframework.aop.scope.ScopedProxyUtils
import org.springframework.beans.BeansException
import org.springframework.beans.PropertyValues
import org.springframework.beans.factory.BeanClassLoaderAware
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.BeanFactoryUtils
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
import org.springframework.beans.factory.config.RuntimeBeanReference
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.DefaultBeanNameGenerator
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.ConfigurationClassPostProcessor
import org.springframework.core.Conventions
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.ResolvableType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import org.springframework.util.ObjectUtils
import org.springframework.util.ReflectionUtils
import org.springframework.util.StringUtils
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap




/**
 * A [BeanFactoryPostProcessor] used to register and inject
 * [MockkBean](@MockkBeans} with the [ApplicationContext]. An initial set of
 * definitions can be passed to the processor with additional definitions being
 * automatically created from `@Configuration` classes that use
 * [MockkBean](@MockBean).
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Andreas Neiser
 * @author JB Nizet
 */
class MockkPostProcessor(private val definitions: Set<Definition>) : InstantiationAwareBeanPostProcessor,
    BeanClassLoaderAware, BeanFactoryAware, BeanFactoryPostProcessor, Ordered {

    private val CONFIGURATION_CLASS_ATTRIBUTE = Conventions.getQualifiedAttributeName(
        ConfigurationClassPostProcessor::class.java,
        "configurationClass"
    )

    private var classLoader: ClassLoader? = null

    private lateinit var beanFactory: BeanFactory

    private val mockkCreatedBeans = MockkCreatedBeans()

    private val beanNameRegistry = HashMap<Definition, String>()

    private val fieldRegistry = HashMap<Field, String>()

    private val spies = HashMap<String, SpykDefinition>()

    override fun setBeanClassLoader(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    @Throws(BeansException::class)
    override fun setBeanFactory(beanFactory: BeanFactory) {
        Assert.isInstanceOf(
            ConfigurableListableBeanFactory::class.java,
            beanFactory,
            "Mockk beans can only be used with a ConfigurableListableBeanFactory"
        )
        this.beanFactory = beanFactory
    }

    @Throws(BeansException::class)
    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        Assert.isInstanceOf(
            BeanDefinitionRegistry::class.java,
            beanFactory,
            "@MockkBean can only be used on bean factories that implement BeanDefinitionRegistry"
        )
        postProcessBeanFactory(beanFactory, beanFactory as BeanDefinitionRegistry)
    }

    private fun postProcessBeanFactory(
        beanFactory: ConfigurableListableBeanFactory,
        registry: BeanDefinitionRegistry
    ) {
        beanFactory.registerSingleton(MockkBeans::class.java.name, this.mockkCreatedBeans)
        val parser = DefinitionsParser(this.definitions)
        for (configurationClass in getConfigurationClasses(beanFactory)) {
            parser.parse(configurationClass)
        }
        val definitions = parser.parsedDefinitions
        for (definition in definitions) {
            val field = parser.getField(definition)
            register(beanFactory, registry, definition, field)
        }
    }

    private fun getConfigurationClasses(
        beanFactory: ConfigurableListableBeanFactory
    ): Set<Class<*>> {
        val configurationClasses = LinkedHashSet<Class<*>>()
        for (beanDefinition in getConfigurationBeanDefinitions(beanFactory).values) {
            beanDefinition.beanClassName?.let {
                configurationClasses.add(ClassUtils.resolveClassName(it, this.classLoader))
            }
        }
        return configurationClasses
    }

    private fun getConfigurationBeanDefinitions(
        beanFactory: ConfigurableListableBeanFactory
    ): Map<String, BeanDefinition> {
        val definitions = LinkedHashMap<String, BeanDefinition>()
        for (beanName in beanFactory.beanDefinitionNames) {
            val definition = beanFactory.getBeanDefinition(beanName)
            definition.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE)?.let {
                definitions[beanName] = definition
            }
        }
        return definitions
    }

    private fun register(
        beanFactory: ConfigurableListableBeanFactory,
        registry: BeanDefinitionRegistry,
        definition: Definition,
        field: Field?
    ) {
        if (definition is MockkDefinition) {
            registerMock(beanFactory, registry, definition, field)
        } else if (definition is SpykDefinition) {
            registerSpy(beanFactory, registry, definition, field)
        }
    }

    private fun registerMock(
        beanFactory: ConfigurableListableBeanFactory,
        registry: BeanDefinitionRegistry,
        definition: MockkDefinition,
        field: Field?
    ) {
        val beanDefinition = createBeanDefinition(definition)
        val beanName = getBeanName(beanFactory, registry, definition, beanDefinition)
        val transformedBeanName = BeanFactoryUtils.transformedBeanName(beanName)
        if (registry.containsBeanDefinition(transformedBeanName)) {
            val existing = registry.getBeanDefinition(transformedBeanName)
            copyBeanDefinitionDetails(existing, beanDefinition)
            registry.removeBeanDefinition(transformedBeanName)
        }
        registry.registerBeanDefinition(transformedBeanName, beanDefinition)
        val mock = definition.createMock<Any>("$beanName bean")
        beanFactory.registerSingleton(transformedBeanName, mock)
        this.mockkCreatedBeans.add(mock)
        this.beanNameRegistry[definition] = beanName
        field?.let {
            this.fieldRegistry[it] = beanName
        }
    }

    private fun createBeanDefinition(mockkDefinition: MockkDefinition): RootBeanDefinition {
        val definition = RootBeanDefinition(
            mockkDefinition.typeToMock.resolve()
        )
        definition.setTargetType(mockkDefinition.typeToMock)
        mockkDefinition.qualifier?.applyTo(definition)
        return definition
    }

    private fun getBeanName(
        beanFactory: ConfigurableListableBeanFactory,
        registry: BeanDefinitionRegistry,
        mockkDefinition: MockkDefinition,
        beanDefinition: RootBeanDefinition
    ): String {
        if (!mockkDefinition.name.isNullOrEmpty()) {
            return mockkDefinition.name
        }
        val existingBeans = getExistingBeans(beanFactory, mockkDefinition.typeToMock, mockkDefinition.qualifier)
        if (existingBeans.isEmpty()) {
            return MockkPostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry)
        }
        if (existingBeans.size == 1) {
            return existingBeans.iterator().next()
        }
        val primaryCandidate = determinePrimaryCandidate(registry, existingBeans, mockkDefinition.typeToMock)
        if (primaryCandidate != null) {
            return primaryCandidate
        }
        throw IllegalStateException(
            "Unable to register mock bean ${mockkDefinition.typeToMock} expected a single matching bean to replace but found $existingBeans"
        )
    }

    private fun copyBeanDefinitionDetails(from: BeanDefinition, to: RootBeanDefinition) {
        to.isPrimary = from.isPrimary
    }

    private fun registerSpy(
        beanFactory: ConfigurableListableBeanFactory,
        registry: BeanDefinitionRegistry,
        spykDefinition: SpykDefinition,
        field: Field?
    ) {
        val existingBeans = getExistingBeans(beanFactory, spykDefinition.typeToSpy, spykDefinition.qualifier)
        if (ObjectUtils.isEmpty(existingBeans)) {
            createSpy(registry, spykDefinition, field)
        } else {
            registerSpies(registry, spykDefinition, field, existingBeans)
        }
    }

    private fun getExistingBeans(
        beanFactory: ConfigurableListableBeanFactory,
        type: ResolvableType, qualifier: QualifierDefinition?
    ): Set<String> {
        val candidates = TreeSet<String>()
        for (candidate in getExistingBeans(beanFactory, type)) {
            if (qualifier == null || qualifier.matches(beanFactory, candidate)) {
                candidates.add(candidate)
            }
        }
        return candidates
    }

    private fun getExistingBeans(
        beanFactory: ConfigurableListableBeanFactory,
        type: ResolvableType
    ): Set<String> {
        val beans = LinkedHashSet(beanFactory.getBeanNamesForType(type, true, false).toList())
        val typeName = type.resolve(Any::class.java).name
        for (beanName in beanFactory.getBeanNamesForType(FactoryBean::class.java, true, false)) {
            val transformedBeanName = BeanFactoryUtils.transformedBeanName(beanName)
            val beanDefinition = beanFactory.getBeanDefinition(transformedBeanName)
            if (typeName == beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE)) {
                beans.add(transformedBeanName)
            }
        }
        beans.removeIf { this.isScopedTarget(it) }
        return beans
    }

    private fun isScopedTarget(beanName: String): Boolean {
        try {
            return ScopedProxyUtils.isScopedTarget(beanName)
        } catch (ex: Throwable) {
            return false
        }
    }

    private fun createSpy(
        registry: BeanDefinitionRegistry, spykDefinition: SpykDefinition,
        field: Field?
    ) {
        val beanDefinition = RootBeanDefinition(spykDefinition.typeToSpy.resolve())
        val beanName = MockkPostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry)
        registry.registerBeanDefinition(beanName, beanDefinition)
        registerSpy(spykDefinition, field, beanName)
    }

    private fun registerSpies(
        registry: BeanDefinitionRegistry,
        spykDefinition: SpykDefinition,
        field: Field?,
        existingBeans: Collection<String>
    ) {
        try {
            val beanName = determineBeanName(existingBeans, spykDefinition, registry)
            beanName?.let {
                registerSpy(spykDefinition, field, it)
            }
        } catch (ex: RuntimeException) {
            throw IllegalStateException("Unable to register spy bean ${spykDefinition.typeToSpy}", ex)
        }
    }

    private fun determineBeanName(
        existingBeans: Collection<String>,
        definition: SpykDefinition,
        registry: BeanDefinitionRegistry
    ): String? {
        if (StringUtils.hasText(definition.name)) {
            return definition.name
        }
        return if (existingBeans.size == 1) {
            existingBeans.iterator().next()
        } else determinePrimaryCandidate(registry, existingBeans, definition.typeToSpy)
    }

    private fun determinePrimaryCandidate(
        registry: BeanDefinitionRegistry,
        candidateBeanNames: Collection<String>,
        type: ResolvableType
    ): String? {
        var primaryBeanName: String? = null
        for (candidateBeanName in candidateBeanNames) {
            val beanDefinition = registry.getBeanDefinition(candidateBeanName)
            if (beanDefinition.isPrimary) {
                if (primaryBeanName != null) {
                    throw NoUniqueBeanDefinitionException(
                        type.resolve()!!,
                        candidateBeanNames.size,
                        "more than one 'primary' bean found among candidates: $candidateBeanNames"
                    )
                }
                primaryBeanName = candidateBeanName
            }
        }
        return primaryBeanName
    }

    private fun registerSpy(definition: SpykDefinition, field: Field?, beanName: String) {
        this.spies[beanName] = definition
        this.beanNameRegistry[definition] = beanName
        if (field != null) {
            this.fieldRegistry[field] = beanName
        }
    }

    protected fun createSpyIfNecessary(bean: Any, beanName: String): Any {
        var spy = bean
        this.spies[beanName]?.let { spy = it.createSpy(beanName, bean) }
        return spy
    }

    override fun postProcessProperties(pvs: PropertyValues, bean: Any, beanName: String): PropertyValues {
        ReflectionUtils.doWithFields(bean.javaClass) { field -> postProcessField(bean, field) }
        return pvs
    }

    private fun postProcessField(bean: Any?, field: Field) {
        val beanName = this.fieldRegistry[field]
        beanName?.let {
            if (StringUtils.hasText(it)) {
                inject(field, bean, it)
            }
        }
    }

    internal fun inject(field: Field, target: Any, definition: Definition) {
        val beanName = this.beanNameRegistry[definition]
        check(beanName != null && StringUtils.hasLength(beanName)) { "No bean found for definition $definition" }
        inject(field, target, beanName)
    }

    private fun inject(field: Field, target: Any?, beanName: String) {
        try {
            field.isAccessible = true
            val existingValue = ReflectionUtils.getField(field, target);
            val bean = this.beanFactory.getBean(beanName, field.type)
            if (existingValue === bean) {
                return;
            }
            check(existingValue == null) {
                "The existing value '${existingValue}' of field '${field}' is not the same as the new value '${bean}'"
            }
            ReflectionUtils.setField(field, target, bean)
        } catch (ex: Throwable) {
            throw BeanCreationException("Could not inject field: $field", ex)
        }

    }

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE - 10
    }

    /**
     * [BeanPostProcessor] to handle [SpykBean] definitions. Registered as a
     * separate processor so that it can be ordered above AOP post processors.
     */
    internal class SpyPostProcessor(private val mockkPostProcessor: MockkPostProcessor) :
        SmartInstantiationAwareBeanPostProcessor,
        PriorityOrdered {

        private val earlySpyReferences: MutableMap<String, Any> = ConcurrentHashMap(16)

        override fun getOrder(): Int {
            return Ordered.HIGHEST_PRECEDENCE
        }

        @Throws(BeansException::class)
        override fun getEarlyBeanReference(bean: Any, beanName: String): Any {
            return if (bean is FactoryBean<*>) {
                bean
            } else {
                this.earlySpyReferences.put(getCacheKey(bean, beanName), bean)
                this.mockkPostProcessor.createSpyIfNecessary(bean, beanName)
            }
        }

        @Throws(BeansException::class)
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
            return if (bean is FactoryBean<*>) {
                bean
            } else if (this.earlySpyReferences.remove(getCacheKey(bean, beanName)) != bean) {
                this.mockkPostProcessor.createSpyIfNecessary(bean, beanName)
            } else {
                bean
            }
        }

        private fun getCacheKey(bean: Any, beanName: String): String {
            return if (StringUtils.hasLength(beanName)) beanName else bean.javaClass.name
        }

        companion object {

            private val BEAN_NAME = SpyPostProcessor::class.java.name

            fun register(registry: BeanDefinitionRegistry) {
                if (!registry.containsBeanDefinition(BEAN_NAME)) {
                    val definition = RootBeanDefinition(
                        SpyPostProcessor::class.java
                    )
                    definition.role = BeanDefinition.ROLE_INFRASTRUCTURE
                    val constructorArguments = definition.constructorArgumentValues
                    constructorArguments.addIndexedArgumentValue(0, RuntimeBeanReference(MockkPostProcessor.BEAN_NAME))
                    registry.registerBeanDefinition(BEAN_NAME, definition)
                }
            }
        }
    }

    companion object {
        private val BEAN_NAME = MockkPostProcessor::class.java.name

        private val beanNameGenerator = DefaultBeanNameGenerator()

        /**
         * Register the processor with a [BeanDefinitionRegistry]. Not required when
         * using the [SpringRunner] as registration is automatic.
         * @param registry the bean definition registry
         * @param postProcessor the post processor class to register
         * @param definitions the initial mock/spy definitions
         */
        @Suppress("UNCHECKED_CAST")
        fun register(
            registry: BeanDefinitionRegistry,
            postProcessor: Class<out MockkPostProcessor> = MockkPostProcessor::class.java,
            definitions: Set<Definition> = emptySet()
        ) {
            SpyPostProcessor.register(registry)
            val definition = getOrAddBeanDefinition(registry, postProcessor)
            val constructorArg = definition.constructorArgumentValues.getIndexedArgumentValue(0, MutableSet::class.java)
            val existing = constructorArg!!.value as MutableSet<Definition>
            existing.addAll(definitions)
        }

        private fun getOrAddBeanDefinition(
            registry: BeanDefinitionRegistry,
            postProcessor: Class<out MockkPostProcessor>
        ): BeanDefinition {
            if (!registry.containsBeanDefinition(BEAN_NAME)) {
                val definition = RootBeanDefinition(postProcessor)
                definition.role = BeanDefinition.ROLE_INFRASTRUCTURE
                val constructorArguments = definition.constructorArgumentValues
                constructorArguments.addIndexedArgumentValue(0, LinkedHashSet<MockkDefinition>())
                registry.registerBeanDefinition(BEAN_NAME, definition)
                return definition
            }
            return registry.getBeanDefinition(BEAN_NAME)
        }
    }
}
