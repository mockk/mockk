# SpringMockK - Spring Boot 2.x Support

Support for Spring Boot 2.x integration tests written in Kotlin using [MockK](https://mockk.io/) instead of Mockito.

Spring Boot provides `@MockBean` and `@SpyBean` annotations for integration tests, which create mock/spy beans using Mockito.

This project provides equivalent annotations `@MockkBean` and `@SpykBean` to do the exact same thing with MockK.

## Version Compatibility

This module supports **Spring Boot 2.x**:

- Spring Framework 5.3.17+
- Spring Boot 2.6.5+
- Java 8+

## Module Structure

This is the SpringMockK module for Spring Boot 2.x. For other Spring Boot versions, see:

- [springmockk](../springmockk) - For Spring Boot 4.x (Spring Framework 7)
- [springmockk-boot3](../springmockk-boot3) - For Spring Boot 3.x

## Principle

All the Mockito-specific classes of the spring-boot-test library, including the automated tests, have been cloned, translated to Kotlin, and adapted to MockK.

This library thus provides the same functionality as the standard Mockito-based Spring Boot mock beans.

## Usage

### Gradle (Kotlin DSL)

Add this to your dependencies:

```kotlin
testImplementation("io.mockk:springmockk-boot2:${mockkVersion}")
```

If you want to make sure Mockito (and the standard `MockBean` and `SpyBean` annotations) is not used, you can also exclude the mockito dependency:

```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(module = "mockito-core")
}
```

### Maven

Add this to your dependencies:

```xml
<dependency>
    <groupId>io.mockk</groupId>
    <artifactId>springmockk-boot2</artifactId>
    <version>${mockkVersion}</version>
    <scope>test</scope>
</dependency>
```

### Example

Here's a complete example using JUnit 5:

```kotlin
@ExtendWith(SpringExtension::class)
@WebMvcTest
class GreetingControllerTest {
    @MockkBean
    private lateinit var greetingService: GreetingService

    @Autowired
    private lateinit var controller: GreetingController

    @Test
    fun `should greet by delegating to the greeting service`() {
        every { greetingService.greet("John") } returns "Hi John"

        assertThat(controller.greet("John")).isEqualTo("Hi John")
        verify { greetingService.greet("John") }
    }
}
```

For JUnit 4, use `@RunWith(SpringRunner::class)` instead of `@ExtendWith(SpringExtension::class)`.

## Differences with Mockito

- The MockK defaults are used, which means that mocks created by the annotations are **strict** (i.e. not relaxed) by default.
  - You can configure MockK globally to use different defaults via [settings file](https://mockk.io/#settings-file)
  - Or use annotation options: `@MockkBean(relaxed = true)` or `@MockkBean(relaxUnitFun = true)`
- The created mocks can't be serializable as they can be with Mockito (MockK doesn't support this feature)

## Gotchas

### JDK Proxy Spying

In some situations, the beans that need to be spied are JDK proxies. In Java 16+, MockK can't spy JDK proxies unless you pass the JVM argument:

```
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
```

Not doing that and trying to spy on a JDK proxy will lead to an error:

```
java.lang.IllegalAccessException: class io.mockk.impl.InternalPlatform cannot access a member of class java.lang.reflect.Proxy (in module java.base) with modifiers "protected"
```

#### Gradle Configuration

```kotlin
tasks.test {
    jvmArgs(
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
    )
}
```

#### Maven Configuration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
      <argLine>
        --add-opens java.base/java.lang.reflect=ALL-UNNAMED
      </argLine>
    </configuration>
</plugin>
```

## Limitations

- The [Spring Boot issue 5837](https://github.com/spring-projects/spring-boot/issues/5837), which has been fixed for Mockito spies using Mockito-specific features, also exists with MockK and hasn't been fixed yet. Contributions are welcome!
- This is not an official Spring Boot project, so it might not work out of the box for newest versions if backwards incompatible changes are introduced in Spring Boot. Please file issues if you find problems.
- Annotations are looked up on fields, and not on properties (for now). This doesn't matter much until you use a custom qualifier annotation. In that case, make sure that it targets fields and not properties, or use `@field:YourQualifier` to apply it on your beans.

## History

This module was incorporated into MockK based on version 4.0.2 of the original SpringMockK project (dcbe643). The original SpringMockK project has been integrated into MockK for continued maintenance by the MockK community.

This specific module maintains compatibility with Spring Boot 2.x, using Spring Framework 5.x under the hood.

## How to Build

```bash
./gradlew :modules:springmockk-boot2:build
```

## Additional Resources

- [MockK Documentation](https://mockk.io/)
- [Spring Boot 2.x Testing Documentation](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/features.html#features.testing)
- [Original SpringMockK Project](https://github.com/Ninja-Squad/springmockk)