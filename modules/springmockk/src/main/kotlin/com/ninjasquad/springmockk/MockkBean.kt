package com.ninjasquad.springmockk

import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.bean.override.BeanOverride
import kotlin.reflect.KClass

/**
 * `@MockkBean` is an annotation that can be used in test classes to
 * override a bean in the test's [ApplicationContext][org.springframework.context.ApplicationContext]
 * with a MockK mock.
 *
 * `@MockkBean` can be applied in the following ways.
 *  - On a non-static field in a test class or any of its superclasses.
 *  - On a non-static field in an enclosing class for a `@Nested` test class
 *    or in any class in the type hierarchy or enclosing class hierarchy above the
 *    `@Nested` test class.
 *  - At the type level on a test class or any superclass or implemented interface
 *    in the type hierarchy above the test class.
 *  - At the type level on an enclosing class for a `@Nested` test class
 *    or on any class or interface in the type hierarchy or enclosing class hierarchy
 *    above the `@Nested` test class.
 *
 * When `@MockkBean` is declared on a field, the bean to mock is inferred
 * from the type of the annotated field. If multiple candidates exist in the
 * `ApplicationContext`, a `@field:Qualifier` annotation can be declared
 * on the field to help disambiguate. In the absence of a `@Qualifier`
 * annotation, the name of the annotated field will be used as a *fallback
 * qualifier*. Alternatively, you can explicitly specify a bean name to mock
 * by setting the [value] or [name] property.
 *
 * When `@MockkBean` is declared at the type level, the type of bean
 * (or beans) to mock must be supplied via the [types] property.
 * If multiple candidates exist in the `ApplicationContext`, you can
 * explicitly specify a bean name to mock by setting the [name]
 * property. Note, however, that the [types] attribute must contain a
 * single type if an explicit bean `name` is configured.
 *
 * A bean will be created if a corresponding bean does not exist. However, if
 * you would like for the test to fail when a corresponding bean does not exist,
 * you can set the [enforceOverride] attribute to `true`
 * &mdash; for example, `@MockkBean(enforceOverride = true)`.
 *
 * Dependencies that are known to the application context but are not beans
 * (such as those
 * [registered directly][org.springframework.beans.factory.config.ConfigurableListableBeanFactory.registerResolvableDependency]) will not be found, and a mocked bean will be added to
 * the context alongside the existing dependency.
 *
 * **WARNING**: Using `@MockkBean` in conjunction with
 * `@ContextHierarchy` can lead to undesirable results since each
 * `@MockkBean` will be applied to all context hierarchy levels by default.
 * To ensure that a particular `@MockkBean` is applied to a single context
 * hierarchy level, set the [contextName] to match a
 * configured `@ContextConfiguration`
 * [name][org.springframework.test.context.ContextConfiguration.name].
 * See the Javadoc for [`@ContextHierarchy`][org.springframework.test.context.ContextHierarchy]
 * for further details and examples.
 *
 * **NOTE**: When mocking a non-singleton bean, the non-singleton
 * bean will be replaced with a singleton mock, and the corresponding bean definition
 * will be converted to a singleton. Consequently, if you mock a prototype or scoped
 * bean, the mock will be treated as a singleton. Similarly, when mocking a bean
 * created by a [FactoryBean][org.springframework.beans.factory.FactoryBean],
 * the `FactoryBean` will be replaced with a singleton mock of the type of
 * object created by the `FactoryBean`.
 *
 * There are no restrictions on the visibility of a `@MockkBean` field.
 * Such fields can therefore be `public`, `protected`, `internal`, or
 * `private` depending on the needs or coding practices of the project.
 *
 * `@MockkBean` fields and type-level `@MockkBean` declarations
 * will be inherited from an enclosing test class by default. See
 * [NestedConfiguration][org.springframework.test.context.NestedTestConfiguration]
 * for details.
 *
 * `@MockkBean` may be used as a *meta-annotation* to create custom
 * *composed annotations* &mdash; for example, to define common mock
 * configuration in a single annotation that can be reused across a test suite.
 * `@MockkBean` can also be used as a *repeatable*
 * annotation at the type level &mdash; for example, to mock several beans by
 * [name].
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 *
 * @see MockkSpyBean
 * @see org.springframework.test.context.bean.override.convention.TestBean
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Repeatable
@BeanOverride(MockkBeanOverrideProcessor::class)
annotation class MockkBean(

    /**
     * Alias for [name].
     *
     * Intended to be used when no other attributes are needed &mdash; for
     * example, `@MockkBean("customBeanName")`.
     *
     * @see name
     */
    @get:AliasFor("name")
    val value: String = "",

    /**
     * Name of the bean to mock.
     *
     * If left unspecified, the bean to mock is selected according to the
     * configured [types] or the annotated field's type, taking
     * qualifiers into account if necessary. See the [class-level documentation][MockkBean]
     * for details.
     * @see value
     */
    @get:AliasFor("value")
    val name: String = "",

    /**
     * One or more types to mock.
     *
     * Defaults to none.
     *
     * Each type specified will result in a mock being created and registered
     * with the `ApplicationContext`.
     *
     * Types must be omitted when the annotation is used on a field.
     *
     * When `@MockkBean` also defines a [name], this attribute
     * can only contain a single value.
     * @return the types to mock
     */
    val types: Array<KClass<*>> = [],

    /**
     * The name of the context hierarchy level in which this `@MockkBean`
     * should be applied.
     *
     * Defaults to an empty string which indicates that this `@MockkBean`
     * should be applied to all application contexts.
     *
     * If a context name is configured, it must match a name configured via
     * `@ContextConfiguration(name=...)`.
     *
     * @see org.springframework.test.context.ContextHierarchy
     * @see org.springframework.test.context.ContextConfiguration.name
     */
    val contextName: String = "",

    /**
     * Extra interfaces that should also be declared by the mock.
     *
     * Defaults to none.
     *
     * @return any extra interfaces
     */
    val extraInterfaces: Array<KClass<*>> = [],

    /**
     * The clear mode to apply to the mock.
     *
     * The default is [MockkClear.AFTER] meaning that mocks are
     * automatically reset after each test method is invoked.
     *
     * @return the clear mode
     */
    val clear: MockkClear = MockkClear.AFTER,

    /**
     * Whether to require the existence of the bean being mocked.
     *
     * Defaults to `false` which means that a mock will be created if a
     * corresponding bean does not exist.
     *
     * Set to `true` to cause an exception to be thrown if a corresponding
     * bean does not exist.
     *
     * @see org.springframework.test.context.bean.override.BeanOverrideStrategy.REPLACE_OR_CREATE
     * @see org.springframework.test.context.bean.override.BeanOverrideStrategy.REPLACE
     */
    val enforceOverride: Boolean = false,

    /**
     * Specifies if the created mock will be relaxed or not
     * @return true if relaxed, false otherwise
     *
     * @see https://mockk.io/#relaxed-mock
     */
    val relaxed: Boolean = false,

    /**
     * Specifies if the created mock will have relaxed `Unit`-returning functions
     * @return true if relaxed, false otherwise
     *
     * @see https://mockk.io/#mock-relaxed-for-functions-returning-unit
     */
    val relaxUnitFun: Boolean = false
)
