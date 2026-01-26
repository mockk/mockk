# Restricted Mocking

**Restricted Mocking** is a feature in MockK designed to **prevent the mocking of classes** that are problematic to mock.
These classes often indicate poor test design and can lead to **unreliable** or **misleading test results**.

The primary goal is to:
- **Encourage better testing practices**
- **Promote code maintainability**
- **Avoid mocking classes tied to system operations or critical data structures**


## Why Restrict Mocking?

Mocking certain classes can cause several issues:

| 🚩 **Problem**              | :warning: **Impact**                                                        |
|-----------------------------|-----------------------------------------------------------------------------|
| **False sense of security** | Tests may pass even when the implementation is fragile or incorrect.        |
| **Tight coupling**          | Tests become overly dependent on low-level implementation details.          |
| **Hard-to-maintain tests**  | Changes in code can break unrelated tests, increasing maintenance overhead. |
| **Code smells**             | Mocking system-level or value-type classes often signals poor architecture. |


## Default Restricted Classes

The following classes are **restricted from being mocked by default**:

| **Class**              | **Description**                                                       | **Includes Subtypes?** |
|------------------------|-----------------------------------------------------------------------|------------------------|
| `java.lang.System`     | System-related APIs (`System.currentTimeMillis()`, `System.getenv()`) | ✅ Yes                  |
| `java.util.Collection` | Collections like `List`, `Set`, and `Queue`                           | ✅ Yes                  |
| `java.util.Map`        | Key-value data structures like `HashMap`                              | ✅ Yes                  |
| `java.io.File`         | File I/O classes (should be abstracted instead)                       | ✅ Yes                  |
| `java.nio.file.Path`   | Path manipulation classes for file systems                            | ✅ Yes                  |


::: info
**All subclasses and implementations** of these classes are also restricted.
:::

For example:
- `ArrayList` and `HashSet` (subtypes of `Collection`)
- `HashMap` (subtype of `Map`)
- Custom classes that extend `File` or implement `Path`

## How to Configure Restricted Mocking

You can configure Restricted Mocking behavior using the `mockk.properties` configuration file described in the [Configuration](configuration) section.

Add the following properties to your configuration file:

```properties
# List of restricted classes (fully qualified names, separated by commas)
mockk.restrictedClasses=com.foo.Bar,com.foo.Baz

# Whether to throw an exception when mocking restricted classes
mockk.throwExceptionOnBadMock=true
```


::: info
If `mockk.throwExceptionOnBadMock` is not set, it will default to `false`, meaning only warnings will be logged.
::: 
To strictly prevent mocking restricted classes, explicitly set:
```properties
mockk.throwExceptionOnBadMock=true
```

## Configure Restricted Mocking with System Properties

You can also configure

```
mockk.throwExceptionOnBadMock=true
```

Example with system properties in your build.gradle.kts file:

```
tasks.withType<Test> {
    systemProperty("mockk.throwExceptionOnBadMock", "true")
}
```
You can also do a variable system property and pass the value
via the terminal like so:

```
tasks.withType<Test> {
    systemProperty("mockk.throwExceptionOnBadMock", System.getProperty("mockk.throwExceptionOnBadMock"))
}
```
Then:
```
./gradlew -Dmockk.throwExceptionOnBadMock=true :modules:name:test
```

## Configure Restricted Mocking with Gradle Properties

You can add the property to your `gradle.properties` file:
```properties
systemProp.mockk.throwExceptionOnBadMock=true
```
Then in your terminal:

```
./gradlew -Pmockk.throwExceptionOnBadMock=true :modules:name:test
```

::: warning
The `mockk.throwExceptionOnBadMock` that is set in `gradle.properties` or
in your `build.gradle` will override the value that is set in `mockk.properties` file.
:::
## Behavior When Mocking Restricted Classes

### When `mockk.throwExceptionOnBadMock=false` (Default)

```kotlin
@Test
fun `when throwExceptionOnBadMock is false should not throw exception for collections`() {
    val mockList = mockk<List<String>>()
    every { mockList.size } returns 0

    mockList.size shouldBe 0
}
```

- Result:
  - A warning log is generated, but the test passes.

- Log Example:
  - List should not be mocked! Consider refactoring your test.

### When `mockk.throwExceptionOnBadMock=true`
```kotlin
@Test
fun `when throwExceptionOnBadMock is true should throw MockKException for collections`() {
    assertThrows<MockKException> {
        mockk<List<String>>()  // Throws MockKException
    }
}
```

- Result:
  - A MockKException is thrown, causing the test to fail.
- Exception Example:
  - MockKException: Mocking java.util.HashMap is not allowed!

## Custom Class Restriction Example

You can restrict **custom classes** from being mocked using the `mockk.properties` configuration file.
This helps enforce proper testing practices even within your own codebase.

### Example 1: Mocking a Restricted Custom Class (Throws Exception)

Add the following to your `mockk.properties` file:

```kotlin
package com.foo

class Foo {
    fun doSomething(): String = "print Foo"
}
```

```kotlin
package com.bar

class Bar {
    fun doSomething(): String = "print Bar"
}

class Baz : Bar() {
    fun doSomething(): String = "print Baz"
}
```

```properties
# Restrict custom classes from being mocked
mockk.restrictedClasses=com.foo.Bar,com.foo.Baz

# Throw an exception when attempting to mock restricted classes
mockk.throwExceptionOnBadMock=true
```

```kotlin
import io.mockk.mockk
import io.mockk.MockKException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RestrictedTest {
    @Test
    fun `should throw exception when mocking restricted class Foo`() {
        assertFailsWith<MockKException> {
            mockk<Foo>()  // 🚫 This will throw an exception
        }
    }

    @Test
    fun `should throw exception when mocking restricted class Bar`() {
        assertFailsWith<MockKException> {
            mockk<Bar>()  // 🚫 This will throw an exception
        }
    }

    @Test
    fun `should throw exception when mocking restricted class Baz`() {
        assertFailsWith<MockKException> {
            mockk<Baz>()  // 🚫 This will throw an exception
        }
    }
}
```
