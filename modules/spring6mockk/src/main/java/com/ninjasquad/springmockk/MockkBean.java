package com.ninjasquad.springmockk;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Annotation that can be used to add MockK mocks to a Spring {@link ApplicationContext}. Can be
 * used as a class level annotation or on fields in either {@code @Configuration} classes,
 * or test classes that are run with the {@link SpringRunner}.
 * <p>
 * Mocks can be registered by type or by {@link #name() bean name}. When registered by
 * type, any existing single bean of a matching type (including subclasses) in the context
 * will be replaced by the mock. When registered by name, an existing bean can be
 * specifically targeted for replacement by a mock. In either case, if no existing bean is
 * defined a new one will be added. Dependencies that are known to the application context
 * but are not beans (such as those
 * {@link org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly}) will not be found and a mocked bean will be added to the context
 * alongside the existing dependency.
 * <p>
 * When {@code @MockkBean} is used on a field, as well as being registered in the
 * application context, the mock will also be injected into the field. Typical usage might
 * be: <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * class ExampleTests {
 *
 *     &#064;MockkBean
 *     private lateinit var service: ExampleService
 *
 *     &#064;Autowired
 *     private lateinit var userOfService: UserOfService
 *
 *     &#064;Test
 *     void testUserOfService() {
 *         every { service.greet() } returns "Hello"
 *         val actual = userOfService.makeUse()
 *         assertThat(actual).isEqualTo("Was: Hello")
 *     }
 *
 *     &#064;Configuration
 *     &#064;Import(UserOfService::class) // A &#064;Component injected with ExampleService
 *     class Config {
 *     }
 *
 *
 * }
 * </pre> If there is more than one bean of the requested type, qualifier metadata must be
 * specified at field level: <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * class ExampleTests {
 *
 *     &#064;MockkBean
 *     &#064;Qualifier("example")
 *     private lateinit var service: ExampleService
 *
 *     ...
 * }
 * </pre>
 * <p>
 * This annotation is {@code @Repeatable} and may be specified multiple times when working
 * with Java 8 or contained within an {@link MockkBeans @MockkBeans} annotation.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see MockkPostProcessor
 */
@Target({ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(MockkBeans.class)
public @interface MockkBean {

    /**
     * The name of the bean to register or replace. If not specified the name will either
     * be generated or, if the mock replaces an existing bean, the existing name will be
     * used.
     * @return the name of the bean
     */
    String name() default "";

    /**
     * The classes to mock. This is an alias of {@link #classes()} which can be used for
     * brevity if no other attributes are defined. See {@link #classes()} for details.
     * @return the classes to mock
     */
    @AliasFor("classes")
    Class<?>[] value() default {};

    /**
     * The classes to mock. Each class specified here will result in a mock being created
     * and registered with the application context. Classes can be omitted when the
     * annotation is used on a field.
     * <p>
     * When {@code @MockkBean} also defines a {@code name} this attribute can only contain
     * a single value.
     * <p>
     * If this is the only specified attribute consider using the {@code value} alias
     * instead.
     * @return the classes to mock
     */
    @AliasFor("value")
    Class<?>[] classes() default {};

    /**
     * Any extra interfaces that should also be declared on the mock.
     * @return any extra interfaces
     */
    Class<?>[] extraInterfaces() default {};

    /**
     * The clear mode to apply to the mock bean. The default is {@link MockkClear#AFTER}
     * meaning that mocks are automatically reset after each test method is invoked.
     * @return the clear mode
     */
    MockkClear clear() default MockkClear.AFTER;

    /**
     * Specifies if the created mock will be relaxed or not
     * @return true if relaxed, false otherwise
     */
    boolean relaxed() default false;

    /**
     * Specifies if the created mock will have relaxed <code>Unit</code>-returning functions
     * @return true if relaxed, false otherwise
     */
    boolean relaxUnitFun() default false;
}
