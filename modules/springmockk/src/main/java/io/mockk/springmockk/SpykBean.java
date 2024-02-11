package io.mockk.springmockk;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to apply MockK spies to a Spring
 * {@link ApplicationContext}. Can be used as a class level annotation or on fields in
 * either {@code @Configuration} classes, or test classes that are
 * run with the {@link SpringRunner}.
 * <p>
 * Spies can be applied by type or by {@link #name() bean name}. All beans in the context
 * of a matching type (including subclasses) will be wrapped with the spy. If no existing
 * bean is defined a new one will be added. Dependencies that are known to the application
 * context but are not beans (such as those
 * {@link org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly}) will not be found and a spied bean will be added to the context
 * alongside the existing dependency.
 * <p>
 * When {@code @SpykBean} is used on a field, as well as being registered in the
 * application context, the spy will also be injected into the field. Typical usage might
 * be: <pre class="code">
 * &#064;RunWith(SpringRunner::class)
 * class ExampleTests {
 *
 *     &#064;SpykBean
 *     private lateinit var service: ExampleService;
 *
 *     &#064;Autowired
 *     private lateinit var userOfService UserOfService;
 *
 *     &#064;Test
 *     fun testUserOfService() {
 *         val actual = userOfService.makeUse()
 *         assertThat(actual).isEqualTo("Was: Hello")
 *         verify { service.greet() }
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
 * public class ExampleTests {
 *
 *     &#064;SpykBean
 *     &#064;Qualifier("example")
 *     private lateinit var service: ExampleService
 *
 *     ...
 * }
 * </pre>
 * <p>
 * This annotation is {@code @Repeatable} and may be specified multiple times when working
 * with Java 8 or contained within a {@link SpykBeans @SpykBeans} annotation.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @see MockkPostProcessor
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(SpykBeans.class)
public @interface SpykBean {

    /**
     * The name of the bean to spy. If not specified the name will either be generated or,
     * if the spy is for an existing bean, the existing name will be used.
     *
     * @return the name of the bean
     */
    String name() default "";

    /**
     * The classes to spy. This is an alias of {@link #classes()} which can be used for
     * brevity if no other attributes are defined. See {@link #classes()} for details.
     *
     * @return the classes to spy
     */
    @AliasFor("classes")
    Class<?>[] value() default {};

    /**
     * The classes to spy. Each class specified here will result in a spy being applied.
     * Classes can be omitted when the annotation is used on a field.
     * <p>
     * When {@code @SpykBean} also defines a {@code name} this attribute can only contain a
     * single value.
     * <p>
     * If this is the only specified attribute consider using the {@code value} alias
     * instead.
     *
     * @return the classes to spy
     */
    @AliasFor("value")
    Class<?>[] classes() default {};

    /**
     * The reset mode to apply to the spied bean. The default is {@link MockkClear#AFTER}
     * meaning that spies are automatically reset after each test method is invoked.
     *
     * @return the reset mode
     */
    MockkClear clear() default MockkClear.AFTER;
}
