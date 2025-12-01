package com.ninjasquad.springmockk

import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.bean.override.BeanOverride
import kotlin.reflect.KClass

/**
 * `@MockKSpyBean` is an annotation that can be used in test classes to
 * override a bean in the test's
 * [ApplicationContext][org.springframework.context.ApplicationContext]
 * with a MockK spy that wraps the original bean instance.
 *
 * `@MockkSpyBean` can be applied in the following ways.
 *
 *  - On a non-static field in a test class or any of its superclasses.
 *  - On a non-static field in an enclosing class for a `@Nested` test class
 *    or in any class in the type hierarchy or enclosing class hierarchy above the
 *    `@Nested` test class.
 *  - At the type level on a test class or any superclass or implemented interface
 *    in the type hierarchy above the test class.
 *  - At the type level on an enclosing class for a `@Nested` test class
 *    or on any class or interface in the type hierarchy or enclosing class hierarchy
 *    above the {@code @Nested} test class.
 *
 * When `@MockkSpyBean` is declared on a field, the bean to spy is
 * inferred from the type of the annotated field. If multiple candidates exist in
 * the `ApplicationContext`, a `@field:Qualifier` annotation can be declared
 * on the field to help disambiguate. In the absence of a `@Qualifier`
 * annotation, the name of the annotated field will be used as a *fallback
 * qualifier*. Alternatively, you can explicitly specify a bean name to spy
 * by setting the [value] or [name] property. If a
 * bean name is specified, it is required that a target bean with that name has
 * been previously registered in the application context.
 *
 * When `@MockkSpyBean` is declared at the type level, the type of bean
 * (or beans) to spy must be supplied via the [types] property.
 * If multiple candidates exist in the `ApplicationContext`, you can
 * explicitly specify a bean name to spy by setting the [name]
 * property. Note, however, that the `types` property must contain a
 * single type if an explicit bean `name` is configured.
 *
 * A spy cannot be created for components which are known to the application
 * context but are not beans &mdash; for example, components
 * [registered directly][org.springframework.beans.factory.config.ConfigurableListableBeanFactory.registerResolvableDependency]
 * as resolvable dependencies.
 *
 * **WARNING**: Using `@MockkSpyBean` in conjunction with
 * `@ContextHierarchy` can lead to undesirable results since each
 * `@MockkSpyBean` will be applied to all context hierarchy levels by default.
 * To ensure that a particular `@Mock`SpyBean` is applied to a single context
 * hierarchy level, set the [contextName] to match a
 * configured `@ContextConfiguration`
 * [name][org.springframework.test.context.ContextConfiguration.name].
 * See the Javadoc for [@ContextHierarchy][org.springframework.test.context.ContextHierarchy]
 * for further details and examples.
 *
 * **NOTE**: When creating a spy for a non-singleton bean, the
 * corresponding bean definition will be converted to a singleton. Consequently,
 * if you create a spy for a prototype or scoped bean, the spy will be treated as
 * a singleton. Similarly, when creating a spy for a
 * [FactoryBean][org.springframework.beans.factory.FactoryBean], a spy will
 * be created for the object created by the `FactoryBean`, not for the
 * `FactoryBean` itself.
 *
 * **WARNING**: `@MockkSpyBean` cannot be used to spy on
 * a scoped proxy &mdash; for example, a bean annotated with
 * [`@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)`][org.springframework.context.annotation.Scope].
 * Any attempt to do so will fail with an exception.
 *
 * There are no restrictions on the visibility of a `@MockkSpyBean` field.
 * Such fields can therefore be `public`, `protected`, `internal, or `private` depending
 * on the needs or coding practices of the project.
 *
 * `@MockkSpyBean` fields and type-level `@MockkSpyBean` declarations
 * will be inherited from an enclosing test class by default. See
 * [@NestedConfiguration][org.springframework.test.context.NestedTestConfiguration]
 * for details.
 *
 * `@MockkSpyBean` may be used as a *meta-annotation* to create
 * custom *composed annotations* &mdash; for example, to define common spy
 * configuration in a single annotation that can be reused across a test suite.
 * `@MockkSpyBean` can also be used as a *repeatable*
 * annotation at the type level &mdash; for example, to spy on several beans by
 * [name].
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 *
 * @see MockkBean
 * @see org.springframework.test.context.bean.override.convention.TestBean
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Repeatable
@BeanOverride(MockkBeanOverrideProcessor::class)
annotation class MockkSpyBean(

    /**
     * Alias for [name].
     *
     * Intended to be used when no other attributes are needed &mdash; for
     * example, `@MockkSpyBean("customBeanName")`.
     *
     * @see name
     */
    @get:AliasFor("name")
    val value: String = "",

    /**
     * Name of the bean to spy.
     *
     * If left unspecified, the bean to spy is selected according to the
     * configured [types] or the annotated field's type, taking
     * qualifiers into account if necessary. See the [class-level documentation][MockkSpyBean]
     * for details.
     * @see #value()
     */
    @get:AliasFor("value")
    val name: String = "",

    /**
     * One or more types to spy.
     *
     * Defaults to none.
     *
     * Each type specified will result in a spy being created and registered
     * with the `ApplicationContext`.
     *
     * Types must be omitted when the annotation is used on a field.
     *
     * When `@MockkSpyBean} also defines a [name], this
     * property can only contain a single value.
     *
     * @return the types to spy
     */
    val types: Array<KClass<*>> = [],

    /**
     * The name of the context hierarchy level in which this `@MockkSpyBean`
     * should be applied.
     *
     * Defaults to an empty string which indicates that this `@MockkSpyBean`
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
     * The clear mode to apply to the spied bean.
     *
     * The default is [MockkClear.AFTER] meaning that spies are automatically
     * cleared after each test method is invoked.
     *
     * @return the clear mode
     */
    val clear: MockkClear = MockkClear.AFTER
)
