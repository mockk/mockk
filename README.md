<div align="center">
    <img src="doc/logo-site.png" width="400" align="center" alt="drawing"/>
    <img src="doc/kotlin-logo.svg" width="400" align="center" alt="drawing"/>
</div>

[![Release Version](https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=release)](https://search.maven.org/#search%7Cga%7C1%7Cmockk)
[![Change log](https://img.shields.io/badge/change%20log-%E2%96%A4-yellow.svg)](https://github.com/mockk/mockk/releases)
[![codecov](https://codecov.io/gh/mockk/mockk/branch/master/graph/badge.svg)](https://codecov.io/gh/mockk/mockk)
[![Android](https://img.shields.io/badge/android-support-green.svg)](https://mockk.io/ANDROID)
[![Matrix tests](https://img.shields.io/badge/matrix-test-e53994.svg)](https://mockk.io/MATRIX)
[![Open Source Helpers](https://www.codetriage.com/mockk/mockk/badges/users.svg)](https://www.codetriage.com/mockk/mockk)

## Getting started

All you need to get started is just to add a dependency to `MockK` library.

#### Gradle/Maven dependency

<table>
<thead><tr><th>Approach</th><th>Instruction</th></tr></thead>
<tr>
<td><img src="doc/gradle.png" alt="Gradle"/></td>
<td>
<pre>
testImplementation "io.mockk:mockk:${mockkVersion}"
</pre>
</td>
</tr>
<tr>
<td><img src="doc/gradle.png" alt="Gradle"/> (Kotlin DSL)</td>
 <td>
  <pre>testImplementation("io.mockk:mockk:${mockkVersion}")</pre>
 </td>
</tr>
<tr>
<td><img src="doc/maven.png" alt="Maven"/></td>
<td>
<pre>
 &lt;dependency&gt;
     &lt;groupId&gt;io.mockk&lt;/groupId&gt;
     &lt;artifactId&gt;mockk-jvm&lt;/artifactId&gt;
     &lt;version&gt;${mockkVersion}&lt;/version&gt;
     &lt;scope&gt;test&lt;/scope&gt;
 &lt;/dependency&gt;
</pre>
</td>
</tr>
<tr>
<td><a href="ANDROID.html"><img align="top" src="doc/robot-small.png" height="20" alt="android"/> Unit</a></td>
<td>
<pre>
testImplementation "io.mockk:mockk-android:${mockkVersion}"
testImplementation "io.mockk:mockk-agent:${mockkVersion}"
</pre>
</td>
</tr>
<tr>
<td><a href="ANDROID.html"><img align="top" src="doc/robot-small.png" height="20" alt="android"/> Instrumented</a></td>
<td>
<pre>
androidTestImplementation "io.mockk:mockk-android:${mockkVersion}"
androidTestImplementation "io.mockk:mockk-agent:${mockkVersion}"
</pre>
</td>
</tr>
</table>

## DSL examples

Simplest example. By default mocks are strict, so you need to provide some behaviour.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

verify { car.drive(Direction.NORTH) }

confirmVerified(car)
```

See the "Features" section below for more detailed examples.

## BDD style (optional)

For teams using Behavior-Driven Development, MockK provides BDD-style aliases

```gradle
testImplementation "io.mockk:mockk:${mockkVersion}"
testImplementation "io.mockk:mockk-bdd:${mockkVersion}"
```

```gradle
androidTestImplementation "io.mockk:mockk-android:${mockkVersion}"
androidTestImplementation "io.mockk:mockk-bdd-android:${mockkVersion}"
```

### BDD aliases

| Standard MockK | BDD style |
|----------------|-----------|
| `every { ... }` | `given { ... }` |
| `coEvery { ... }` | `coGiven { ... }` |
| `verify { ... }` | `then { ... }` |
| `coVerify { ... }` | `coThen { ... }` |


### Spring support

 * [springmockk](https://github.com/Ninja-Squad/springmockk) introduced in official [Spring Boot Kotlin tutorial](https://spring.io/guides/tutorials/spring-boot-kotlin/)

### Quarkus support

 * [quarkus-mockk](https://github.com/quarkiverse/quarkus-mockk) adds support for mocking beans in Quarkus. Documentation can be found [here](https://quarkiverse.github.io/quarkiverse-docs/quarkus-mockk/dev/index.html)

### Kotlin version support

From version 1.13.0 MockK supports Kotlin 1.4 and higher

### Known issues

* PowerMock needs a workaround to run together with MockK [#79](https://github.com/mockk/mockk/issues/79#issuecomment-437646333). (not sure after workaround if it is generally usable or not, please somebody report it)
* Inline functions cannot be mocked: see the discussion on [this issue](https://github.com/mockk/mockk/issues/27)
* Spies, `mockkStatic` may not work on JDK 16+; `InaccessibleObjectException`/`IllegalAccessException`: [read more here](doc/md/jdk16-access-exceptions.md)
* Using a spy with a suspending function [will give unexpected test results](https://github.com/mockk/mockk/issues/554)

Table of contents:

* auto-gen TOC:
{:toc}

## Examples, guides & articles

### Kotlin Academy articles <img src="https://cdn-images-1.medium.com/letterbox/47/47/50/50/1*FUXqI88mttV_kV8aTrKjOg.png?source=logoAvatar-1f9f77b4b3d1---e57b304801ef" width="20px" />

Check the series of articles "Mocking is not rocket science" at [Kt. Academy](https://blog.kotlin-academy.com) describing MockK from the very basics of mocking up to description of all advanced features.

 - [Basics](https://blog.kotlin-academy.com/mocking-is-not-rocket-science-basics-ae55d0aadf2b)
 - [Expected behavior and behavior verification](https://blog.kotlin-academy.com/mocking-is-not-rocket-science-expected-behavior-and-behavior-verification-3862dd0e0f03)
 - [MockK features](https://blog.kotlin-academy.com/mocking-is-not-rocket-science-mockk-features-e5d55d735a98)
 - [MockK advanced features](https://blog.kotlin-academy.com/mocking-is-not-rocket-science-mockk-advanced-features-42277e5983b5)

 - [Testing Quarkus with Kotlin, JUnit and MockK](https://www.novatec-gmbh.de/en/blog/testing-quarkus-with-kotlin-junit-and-mockk/)
 - [Unraveling MockK's black magic(EN, translation)](https://chao2zhang.medium.com/unraveling-mockks-black-magic-e725c61ed9dd)
 - [Mockk Guidebook](https://notwoods.github.io/mockk-guidebook/)
 - [“Kotlin Unit Testing with Mockk” by Marco Cattaneo](https://link.medium.com/ObtQ4eBfg5)
 - [(Video) Use verify in MockK to validate function calls on mocked object](https://www.youtube.com/watch?v=J7_4WrImJPk)
 - [Testing With MockK paid course on raywenderlich.com](https://www.raywenderlich.com/5443751-testing-with-mockk)
 - TDD for Android video tutorial [part 1](https://www.youtube.com/watch?v=60KFJTb_HwU), [part 2](https://www.youtube.com/watch?v=32pnzGirvgM) by Ryan Kay
 - [(Video)Android Developer Live Coding #13: Unit Testing with Mockk, Coroutines, Test Driven Development](https://www.youtube.com/watch?v=h8_LZn1DFDI)
 - [KotlinConf 2018 - Best Practices for Unit Testing in Kotlin by Philipp Hauer](https://www.youtube.com/watch?v=RX_g65J14H0&feature=youtu.be&t=940)
 - [kotlin-fullstack-sample uses MockK](https://github.com/Kotlin/kotlin-fullstack-sample/pull/28/files#diff-eade18fbfd0abfb6338dbfa647b3215dR17) project covered with tests
 - [DZone article](https://dzone.com/articles/new-mocking-tool-for-kotlin-an-alternative-to-java)
 - [Habrahabr article](https://habrahabr.ru/post/341202/) (RU)
 - [Mocking in Kotlin with MockK - Yannick De Turck](https://ordina-jworks.github.io/testing/2018/02/05/Writing-tests-in-Kotlin-with-MockK.html)
 - [How does MockK work internally? - Sagar Malhotra](https://proandroiddev.com/how-does-mocking-work-internally-a-deep-dive-into-mockk-internals-defecd66b815)

### Japanese guides and articles
 - [Documentation translation to Japanese](https://qiita.com/yasuX/items/d3cfc9853c53dfaee222)
 - [Unraveling MockK's black magic / MockKの「黒魔術」を解明する (JP, but readable through chrome translator)](https://zenn.dev/oboenikui/articles/af44c158f9fa35)

### Chinese guides and articles
 - [用 Kotlin + Mockito 寫單元測試會碰到什麼問題？](https://medium.com/joe-tsai/mockk-%E4%B8%80%E6%AC%BE%E5%BC%B7%E5%A4%A7%E7%9A%84-kotlin-mocking-library-part-1-4-39a85e42b8)
 - [MockK 功能介紹：mockk, every, Annotation, verify](https://medium.com/joe-tsai/mockk-%E4%B8%80%E6%AC%BE%E5%BC%B7%E5%A4%A7%E7%9A%84-kotlin-mocking-library-part-2-4-4be059331110)
 - [MockK 功能介紹：Relaxed Mocks, 再談 Verify, Capture](https://medium.com/joe-tsai/mockk-%E4%B8%80%E6%AC%BE%E5%BC%B7%E5%A4%A7%E7%9A%84-kotlin-mocking-library-part-3-4-79b40fb73964)
 - [如何測試 Static Method, Singleton](https://medium.com/joe-tsai/mockk-%E4%B8%80%E6%AC%BE%E5%BC%B7%E5%A4%A7%E7%9A%84-kotlin-mocking-library-part-4-4-f82443848a3a)

### Korean guides and articles
 - [Documentation translation to Korean](https://www.devkuma.com/docs/kotlin/mockk/)
 - [MockK의 흑마술을 파헤치자!](https://sukyology.medium.com/mockk%EC%9D%98-%ED%9D%91%EB%A7%88%EC%88%A0%EC%9D%84-%ED%8C%8C%ED%97%A4%EC%B9%98%EC%9E%90-6fe907129c19)

## Features

### Annotations

You can use annotations to simplify the creation of mock objects:

```kotlin

class TrafficSystem {
  lateinit var car1: Car

  lateinit var car2: Car

  lateinit var car3: Car
}

class CarTest {
  @MockK
  lateinit var car1: Car

  @RelaxedMockK
  lateinit var car2: Car

  @MockK(relaxUnitFun = true)
  lateinit var car3: Car

  @SpyK
  var car4 = Car()

  @InjectMockKs
  var trafficSystem = TrafficSystem()

  @Before
  fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

  @Test
  fun calculateAddsValues1() {
      // ... use car1, car2, car3 and car4
  }
}
```

Injection first tries to match properties by name, then by class or superclass.
Check the `lookupType` parameter for customization.

Properties are injected even if `private` is applied. Constructors for injection are selected from the biggest
number of arguments to lowest.

`@InjectMockKs` by default injects only `lateinit var`s or `var`s that are not assigned.
To change this, use `overrideValues = true`. This would assign the value even if it is already initialized somehow.
To inject `val`s, use `injectImmutable = true`. For a shorter notation use `@OverrideMockKs` which does the same as
`@InjectMockKs` by default, but turns these two flags on.

### JUnit4

JUnit 4 exposes a rule-based API to allow for some automation following the test lifecycle. MockK includes a rule which uses this to set up and tear down your mocks without needing to manually call `MockKAnnotations.init(this)`. Example:

```kotlin
class CarTest {
  @get:Rule
  val mockkRule = MockKRule(this)

  @MockK
  lateinit var car1: Car

  @RelaxedMockK
  lateinit var car2: Car

  @Test
  fun something() {
     every { car1.drive() } just runs
     every { car2.changeGear(any()) } returns true
     // etc
  }
}
```

#### JUnit5

In JUnit5 you can use `MockKExtension` to initialize your mocks.

```kotlin
@ExtendWith(MockKExtension::class)
class CarTest {
  @MockK
  lateinit var car1: Car

  @RelaxedMockK
  lateinit var car2: Car

  @MockK(relaxUnitFun = true)
  lateinit var car3: Car

  @SpyK
  var car4 = Car()

  @Test
  fun calculateAddsValues1() {
      // ... use car1, car2, car3 and car4
  }
}
```

Additionally, it adds the possibility to use `@MockK` and `@RelaxedMockK` on test function parameters:

```kotlin
@Test
fun calculateAddsValues1(@MockK car1: Car, @RelaxedMockK car2: Car) {
  // ... use car1 and car2
}
```

Finally, this extension will call `unmockkAll` and `clearAllMocks` in a `@AfterAll` callback, ensuring your test environment is clean after
each test class execution.
You can disable this behavior by adding the `@MockKExtension.KeepMocks` annotation to your class or globally by setting
the `mockk.junit.extension.keepmocks=true` property.
(Since v1.13.11)
Alternatively, since `clearAllMocks` by default (`currentThreadOnly=false`) is not thread-safe, if you need to run test in parallel you can add the
`MockKExtension.RequireParallelTesting` annotation to your class or set the `mockk.junit.extension.requireParallelTesting=true`
property to disable calling it in the `@AfterAll` callback.
If `clearAllMocks` is explicitly called, you can supply `clearAllMocks(currentThreadOnly = true)` so that it only clears mocks created within the same thread (since v1.13.12).

#### Automatic verification confirmation

You can make sure that all stubbed methods are actually verified by also annotating your test class with `@MockKExtension.ConfirmVerification`.

This will internally call `confirmVerified` on all mocks after each test, to make sure there are no unnecessary stubbings.

Please note that this behavior may not work as expected when running tests in your IDE, as it is Gradle who takes care of handling the exception being thrown when these `confirmVerified` calls fail.

#### Automatic unnecessary stubbing check

You can make sure that all stubbed methods are useful - used at least once - by also annotating your test class with `@MockKExtension.CheckUnnecessaryStub`.

This will internally call `checkUnnecessaryStub` on all mocks after each test, to make sure there are no unnecessary stubbings.


### Spy

Spies allow you to mix mocks and real objects.

```kotlin
val car = spyk(Car()) // or spyk<Car>() to call the default constructor

car.drive(Direction.NORTH) // returns whatever the real function of Car returns

verify { car.drive(Direction.NORTH) }

confirmVerified(car)
```

Note 1: the spy object is a copy of the passed object.
Note 2: there is a known issue if using a spy with a suspending function: https://github.com/mockk/mockk/issues/554

### Relaxed mock

A `relaxed mock` is the mock that returns some simple value for all functions.
This allows you to skip specifying behavior for each case, while still stubbing things you need.
For reference types, chained mocks are returned.

```kotlin
val car = mockk<Car>(relaxed = true)

car.drive(Direction.NORTH) // returns null

verify { car.drive(Direction.NORTH) }

confirmVerified(car)
```

Note: relaxed mocking is working badly with generic return types. A class cast exception is usually thrown in this case.
Opt for stubbing manually in the case of a generic return type.

Workaround:

```kotlin
val func = mockk<() -> Car>(relaxed = true) // in this case invoke function has generic return type

// this line is workaround, without it the relaxed mock would throw a class cast exception on the next line
every { func() } returns Car() // or you can return mockk() for example

func()
```

### Partial mocking

Sometimes, you need to stub some functions, but still call the real method on others, or on specific arguments.
This is possible by passing `callOriginal()` to `answers`, which works for both relaxed and non-relaxed mocks.

```kotlin
class Adder {
 fun addOne(num: Int) = num + 1
}

val adder = mockk<Adder>()

every { adder.addOne(any()) } returns -1
every { adder.addOne(3) } answers { callOriginal() }

assertEquals(-1, adder.addOne(2))
assertEquals(4, adder.addOne(3)) // original function is called
```

### Mock relaxed for functions returning Unit

If you want `Unit`-returning functions to be relaxed, you can use `relaxUnitFun = true` as an argument to the `mockk` function,
`@MockK`annotation or `MockKAnnotations.init` function.

Function:
```kotlin
mockk<ClassBeingMocked>(relaxUnitFun = true)
```

Annotation:
```kotlin
@MockK(relaxUnitFun = true)
lateinit var mock1: ClassBeingMocked
init {
    MockKAnnotations.init(this)
}
```

MockKAnnotations.init:
```kotlin
@MockK
lateinit var mock2: ClassBeingMocked
init {
    MockKAnnotations.init(this, relaxUnitFun = true)
}
```

### Object mocks

Objects can be turned into mocks in the following way:

```kotlin
object ObjBeingMocked {
  fun add(a: Int, b: Int) = a + b
}

mockkObject(ObjBeingMocked) // applies mocking to an Object

assertEquals(3, ObjBeingMocked.add(1, 2))

every { ObjBeingMocked.add(1, 2) } returns 55

assertEquals(55, ObjBeingMocked.add(1, 2))
```

To revert back, use `unmockkObject` or `unmockkAll` (more destructive: cancels object, static and constructor mocks)

```kotlin
@Before
fun beforeTests() {
    mockkObject(ObjBeingMocked)
    every { ObjBeingMocked.add(1,2) } returns 55
}

@Test
fun willUseMockBehaviour() {
    assertEquals(55, ObjBeingMocked.add(1,2))
}

@After
fun afterTests() {
    unmockkObject(ObjBeingMocked)
    // or unmockkAll()
}
```

Despite the Kotlin language restrictions, you can create new instances of objects if required by testing logic:
```kotlin
val newObjectMock = mockk<ObjBeingMocked>()
```

### Class mock

Sometimes you need a mock of an arbitrary class. Use `mockkClass` in those cases.

```kotlin
val car = mockkClass(Car::class)

every { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

verify { car.drive(Direction.NORTH) }
```

### Enumeration mocks

Enums can be mocked using `mockkObject`:

```kotlin
enum class Enumeration(val goodInt: Int) {
    CONSTANT(35),
    OTHER_CONSTANT(45);
}

mockkObject(Enumeration.CONSTANT)
every { Enumeration.CONSTANT.goodInt } returns 42
assertEquals(42, Enumeration.CONSTANT.goodInt)
```

### Constructor mocks

Sometimes, especially in code you don't own, you need to mock newly created objects.
For this purpose, the following constructs are provided:

```kotlin
class MockCls {
  fun add(a: Int, b: Int) = a + b
}

mockkConstructor(MockCls::class)

every { anyConstructed<MockCls>().add(1, 2) } returns 4

assertEquals(4, MockCls().add(1, 2)) // note new object is created

verify { anyConstructed<MockCls>().add(1, 2) }
```

The basic idea is that just after the constructor of the mocked class is executed (any of them), objects become a `constructed mock`.
Mocking behavior of such a mock is connected to the special `prototype mock` denoted by `anyConstructed<MockCls>()`.
There is one instance per class of such a `prototype mock`. Call recording also happens to the `prototype mock`.
If no behavior for the function is specified, then the original function is executed.

In case a class has more than one constructor, each can be mocked separately:

```kotlin
class MockCls(private val a: Int = 0) {
  constructor(x: String) : this(x.toInt())
  fun add(b: Int) = a + b
}

mockkConstructor(MockCls::class)

every { constructedWith<MockCls>().add(1) } returns 2
every {
    constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).add(2) // Mocks the constructor which takes a String
} returns 3
every {
    constructedWith<MockCls>(EqMatcher(4)).add(any()) // Mocks the constructor which takes an Int
} returns 4

assertEquals(2, MockCls().add(1))
assertEquals(3, MockCls("2").add(2))
assertEquals(4, MockCls(4).add(7))

verify {
    constructedWith<MockCls>().add(1)
    constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).add(2)
    constructedWith<MockCls>(EqMatcher(4)).add(7)
}
```

Note that in this case, a `prototype mock` is created for every set of argument matchers passed to `constructedWith`.
This means that when verifying invocations on a `prototype mock` you have to ensure that the argument matchers used are
the same that where used when stubbing, i.e. the matchers in the `verify` block and in the `every` block must be the
same.


### Partial argument matching

You can mix both regular arguments and matchers:

```kotlin
val car = mockk<Car>()

every {
  car.recordTelemetry(
    speed = more(50),
    direction = Direction.NORTH, // here eq() is used
    lat = any(),
    long = any()
  )
} returns Outcome.RECORDED

car.recordTelemetry(60, Direction.NORTH, 51.1377382, 17.0257142)

verify { car.recordTelemetry(60, Direction.NORTH, 51.1377382, 17.0257142) }

confirmVerified(car)
```

### Chained calls

You can stub chains of calls:

```kotlin
val car = mockk<Car>()

every { car.door(DoorType.FRONT_LEFT).windowState() } returns WindowState.UP

car.door(DoorType.FRONT_LEFT) // returns chained mock for Door
car.door(DoorType.FRONT_LEFT).windowState() // returns WindowState.UP

verify { car.door(DoorType.FRONT_LEFT).windowState() }

confirmVerified(car)
```

Note: if the function's return type is generic then the information about the actual type is gone.
To make chained calls work, additional information is required.
Most of the time the framework will catch the cast exception and do `autohinting`.
In the case it is explicitly required, use `hint` before making the next call.

```kotlin
every { obj.op2(1, 2).hint(Int::class).op1(3, 4) } returns 5
```

### Hierarchical mocking

From version 1.9.1 mocks may be chained into hierarchies:

```kotlin
interface AddressBook {
    val contacts: List<Contact>
}

interface Contact {
    val name: String
    val telephone: String
    val address: Address
}

interface Address {
    val city: String
    val zip: String
}

val addressBook = mockk<AddressBook> {
    every { contacts } returns listOf(
        mockk {
            every { name } returns "John"
            every { telephone } returns "123-456-789"
            every { address.city } returns "New-York"
            every { address.zip } returns "123-45"
        },
        mockk {
            every { name } returns "Alex"
            every { telephone } returns "789-456-123"
            every { address } returns mockk {
                every { city } returns "Wroclaw"
                every { zip } returns "543-21"
            }
        }
    )
}
```

### Capturing

You can capture an argument to a `CapturingSlot` or `MutableList`.

`CapturingSlot` is usually created via factory method `slot<T : Any?>()` and is possible to capture nullable and non nullable types.
`MutableList` is intended for capturing multiple values during testing.

```kotlin
enum class Direction { NORTH, SOUTH }
enum class RecordingOutcome { RECORDED }
enum class RoadType { HIGHWAY }
class Car {
    fun recordTelemetry(speed: Double, direction: Direction, roadType: RoadType?): RecordingOutcome {
        TODO("not implement for showcase")
    }
}

val car = mockk<Car>()
// allow to capture parameter with non nullable type `Double`
val speedSlot = slot<Double>()
// allow to capture parameter with nullable type `RoadType`
val roadTypeSlot = slot<RoadType?>()
val list = mutableListOf<Double>()

every {
    car.recordTelemetry(
        speed = capture(speedSlot), // makes mock match calls with any value for `speed` and record it in a slot
        direction = Direction.NORTH, // makes mock and capturing only match calls with specific `direction`. Use `any()` to match calls with any `direction`
        roadType = captureNullable(roadTypeSlot), // makes mock match calls with any value for `roadType` and record it in a slot
    )
} answers {
    println("Speed: ${speedSlot.captured}, roadType: ${roadTypeSlot.captured}")

    RecordingOutcome.RECORDED
}

every {
    car.recordTelemetry(
        speed = capture(list),
        direction = Direction.SOUTH,
        roadType = captureNullable(roadTypeSlot),
    )
} answers {
    println("Speed: ${list}, roadType: ${roadTypeSlot.captured}")

    RecordingOutcome.RECORDED
}

car.recordTelemetry(speed = 15.0, direction = Direction.NORTH, null) // prints Speed: 15.0, roadType: null
car.recordTelemetry(speed = 16.0, direction = Direction.SOUTH, RoadType.HIGHWAY) // prints Speed: [16.0], roadType: HIGHWAY

verifyOrder {
    car.recordTelemetry(speed = or(15.0, 16.0), direction = any(), roadType = null)
    car.recordTelemetry(speed = 16.0, direction = any(), roadType = RoadType.HIGHWAY)
}

confirmVerified(car)
```

### Verification atLeast, atMost or exactly times

You can check the call count with the `atLeast`, `atMost` or `exactly` parameters:

```kotlin

val car = mockk<Car>(relaxed = true)

car.accelerate(fromSpeed = 10, toSpeed = 20)
car.accelerate(fromSpeed = 10, toSpeed = 30)
car.accelerate(fromSpeed = 20, toSpeed = 30)

// all pass
verify(atLeast = 3) { car.accelerate(allAny()) }
verify(atMost  = 2) { car.accelerate(fromSpeed = 10, toSpeed = or(20, 30)) }
verify(exactly = 1) { car.accelerate(fromSpeed = 10, toSpeed = 20) }
verify(exactly = 0) { car.accelerate(fromSpeed = 30, toSpeed = 10) } // means no calls were performed

confirmVerified(car)
```

Or you can use `verifyCount`:

```kotlin

val car = mockk<Car>(relaxed = true)

car.accelerate(fromSpeed = 10, toSpeed = 20)
car.accelerate(fromSpeed = 10, toSpeed = 30)
car.accelerate(fromSpeed = 20, toSpeed = 30)

// all pass
verifyCount {
    (3..5) * { car.accelerate(allAny(), allAny()) } // same as verify(atLeast = 3, atMost = 5) { car.accelerate(allAny(), allAny()) }
    1 * { car.accelerate(fromSpeed = 10, toSpeed = 20) } // same as verify(exactly = 1) { car.accelerate(fromSpeed = 10, toSpeed = 20) }
    0 * { car.accelerate(fromSpeed = 30, toSpeed = 10) } // same as verify(exactly = 0) { car.accelerate(fromSpeed = 30, toSpeed = 10) }
}

confirmVerified(car)
```

### Verification order

* `verifyAll` verifies that all calls happened without checking their order.
* `verifySequence` verifies that the calls happened in a specified sequence.
* `verifyOrder` verifies that calls happened in a specific order.
* `wasNot Called` verifies that the mock (or the list of mocks) was not called at all.

```kotlin
class MockedClass {
    fun sum(a: Int, b: Int) = a + b
}

val obj = mockk<MockedClass>()
val slot = slot<Int>()

every {
    obj.sum(any(), capture(slot))
} answers {
    1 + firstArg<Int>() + slot.captured
}

obj.sum(1, 2) // returns 4
obj.sum(1, 3) // returns 5
obj.sum(2, 2) // returns 5

verifyAll {
    obj.sum(1, 3)
    obj.sum(1, 2)
    obj.sum(2, 2)
}

verifySequence {
    obj.sum(1, 2)
    obj.sum(1, 3)
    obj.sum(2, 2)
}

verifyOrder {
    obj.sum(1, 2)
    obj.sum(2, 2)
}

val obj2 = mockk<MockedClass>()
val obj3 = mockk<MockedClass>()
verify {
    listOf(obj2, obj3) wasNot Called
}

confirmVerified(obj)
```

### Verification confirmation

To double-check that all calls were verified by `verify...` constructs, you can use `confirmVerified`:

```kotlin
confirmVerified(mock1, mock2)
```

Since v1.14.6 you can pass `clear = true` to also clear verification marks and recorded calls for the provided mocks after confirmation.

```kotlin
confirmVerified(mock1, mock2, clear = true)
```

It doesn't make much sense to use it for `verifySequence` and `verifyAll`, as these verification methods already exhaustively cover all calls with verification.

It will throw an exception if there are some calls left without verification.

Some calls can be excluded from this confirmation, check the next section for more details.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK
every { car.drive(Direction.SOUTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK
car.drive(Direction.SOUTH) // returns OK

verify {
    car.drive(Direction.SOUTH)
    car.drive(Direction.NORTH)
}

confirmVerified(car) // makes sure all calls were covered with verification
confirmVerified(car, clear = true) // makes sure all calls were covered with verification and clears verification marks and recorded calls
```

### Unnecessary stubbing

Because clean & maintainable test code requires zero unnecessary code, you can ensure that there is no unnecessary stubs.

```kotlin
checkUnnecessaryStub(mock1, mock2)
```

It will throw an exception if there are some declared calls on the mocks that are not used by the tested code.
This can happen if you have declared some really unnecessary stubs or if the tested code doesn't call an expected one.


### Recording exclusions

To exclude unimportant calls from being recorded, you can use `excludeRecords`:

```kotlin
excludeRecords { mock.operation(any(), 5) }
```

All matching calls will be excluded from recording. This may be useful if you are using exhaustive verification: `verifyAll`, `verifySequence` or `confirmVerified`.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK
every { car.drive(Direction.SOUTH) } returns Outcome.OK

excludeRecords { car.drive(Direction.SOUTH) }

car.drive(Direction.NORTH) // returns OK
car.drive(Direction.SOUTH) // returns OK

verify {
    car.drive(Direction.NORTH)
}

confirmVerified(car) // car.drive(Direction.SOUTH) was excluded, so confirmation is fine with only car.drive(Direction.NORTH)
```

### Verification timeout

To verify concurrent operations, you can use `timeout = xxx`:

```kotlin
mockk<MockCls> {
    every { sum(1, 2) } returns 4

    Thread {
        Thread.sleep(2000)
        sum(1, 2)
    }.start()

    verify(timeout = 3000) { sum(1, 2) }
}
```

This will wait until one of two following states: either verification is passed or the timeout is reached.

### Returning Unit

If a function returns `Unit`, you can use the `justRun` construct:

```kotlin
class MockedClass {
    fun sum(a: Int, b: Int): Unit {
        println(a + b)
    }
}

val obj = mockk<MockedClass>()

justRun { obj.sum(any(), 3) }

obj.sum(1, 1)
obj.sum(1, 2)
obj.sum(1, 3)

verify {
    obj.sum(1, 1)
    obj.sum(1, 2)
    obj.sum(1, 3)
}
```

Other ways to write `justRun { obj.sum(any(), 3) }`:
 - `every { obj.sum(any(), 3) } just Runs`
 - `every { obj.sum(any(), 3) } returns Unit`
 - `every { obj.sum(any(), 3) } answers { Unit }`

### Coroutines

To mock coroutines you need to add another dependency to the support library.
<table>
<tr>
    <th>Gradle</th>
</tr>
<tr>
    <td>
<pre>testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:x.x"</pre>
    </td>
</tr>
</table>
<table>
<tr>
    <th>Maven</th>
</tr>
<tr>
<td>
    <pre>
&lt;dependency&gt;
    &lt;groupId&gt;org.jetbrains.kotlinx&lt;/groupId&gt;
    &lt;artifactId&gt;kotlinx-coroutines-core&lt;/artifactId&gt;
    &lt;version&gt;x.x&lt;/version&gt;
    &lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;</pre>
    </td>
</tr>
</table>

Then you can use `coEvery`, `coVerify`, `coMatch`, `coAssert`, `coRun`, `coAnswers` or `coInvoke` to mock suspend functions.

```kotlin
val car = mockk<Car>()

coEvery { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

coVerify { car.drive(Direction.NORTH) }
```

And to simulate a never returning `suspend` function, you can use `coJustAwait`:

```kotlin
runTest {
    val car = mockk<Car>()

    coJustAwait { car.drive(any()) } // car.drive(...) will never return

    val job = launch(UnconfinedTestDispatcher()) {
        car.drive(Direction.NORTH)
    }

    coVerify { car.drive(Direction.NORTH) }

    job.cancelAndJoin() // Don't forget to cancel the job
}
```

Note: there is a known issue if using a spy with a suspending function: https://github.com/mockk/mockk/issues/554

### Top Level functions

Kotlin lets you declare functions that don’t belong to any class or object, called top-level functions. These calls are translated to static methods in `jvm` environments, and a special Java class is generated to hold the functions. These top-level functions can be mocked using `mockkStatic`. You just need to import the function and pass a reference as the argument:

```kotlin
import com.cars.buildCar

val testCar = Car()
mockkStatic(::buildCar)
every { buildCar() } returns testCar

assertEquals(testCar, buildCar())

verify { buildCar() }
```

Mocking a function will clear any existing mocks of other functions declared in the same file, equivalent to calling `clearStaticMockk` on the generated enclosing class.

### Extension functions

There are three types of extension function in Kotlin:

* class-wide
* object-wide
* module-wide

For an object or a class, you can mock extension functions just by creating a regular `mockk`:

```kotlin
data class Obj(val value: Int)

class Ext {
    fun Obj.extensionFunc() = value + 5
}

with(mockk<Ext>()) {
    every {
        Obj(5).extensionFunc()
    } returns 11

    assertEquals(11, Obj(5).extensionFunc())

    verify {
        Obj(5).extensionFunc()
    }
}
```

To mock module-wide extension functions you need to
build `mockkStatic(...)` with the module's class name as an argument.
For example "pkg.FileKt" for module `File.kt` in the `pkg` package.

```kotlin
data class Obj(val value: Int)

// declared in File.kt ("pkg" package)
fun Obj.extensionFunc() = value + 5

mockkStatic("pkg.FileKt")

every {
    Obj(5).extensionFunc()
} returns 11

assertEquals(11, Obj(5).extensionFunc())

verify {
    Obj(5).extensionFunc()
}
```

In `jvm` environments you can replace the class name with a function reference:
```kotlin
mockkStatic(Obj::extensionFunc)
```
Note that this will mock the whole `pkg.FileKt` class, and not just `extensionFunc`.

This syntax also applies for extension properties:
```kotlin
val Obj.squareValue get() = value * value

mockkStatic(Obj::squareValue)
```

If `@JvmName` is used, specify it as a class name.

KHttp.kt:
```kotlin
@file:JvmName("KHttp")

package khttp
// ... KHttp code
```

Testing code:
```kotlin
mockkStatic("khttp.KHttp")
```

Sometimes you need to know a little bit more to mock an extension function.
For example the extension function `File.endsWith()` has a totally unpredictable `classname`:
```kotlin
mockkStatic("kotlin.io.FilesKt__UtilsKt")
every { File("abc").endsWith(any<String>()) } returns true
println(File("abc").endsWith("abc"))
```
This is standard Kotlin behaviour that may be unpredictable.
Use `Tools -> Kotlin -> Show Kotlin Bytecode` or check `.class` files in JAR archive to detect such names.

### Varargs

From version 1.9.1, more extended vararg handling is possible:

```kotlin
interface ClsWithManyMany {
    fun manyMany(vararg x: Any): Int
}

val obj = mockk<ClsWithManyMany>()

every { obj.manyMany(5, 6, *varargAll { it == 7 }) } returns 3

println(obj.manyMany(5, 6, 7)) // 3
println(obj.manyMany(5, 6, 7, 7)) // 3
println(obj.manyMany(5, 6, 7, 7, 7)) // 3

every { obj.manyMany(5, 6, *anyVararg(), 7) } returns 4

println(obj.manyMany(5, 6, 1, 7)) // 4
println(obj.manyMany(5, 6, 2, 3, 7)) // 4
println(obj.manyMany(5, 6, 4, 5, 6, 7)) // 4

every { obj.manyMany(5, 6, *varargAny { nArgs > 5 }, 7) } returns 5

println(obj.manyMany(5, 6, 4, 5, 6, 7)) // 5
println(obj.manyMany(5, 6, 4, 5, 6, 7, 7)) // 5

every {
    obj.manyMany(5, 6, *varargAny {
        if (position < 3) it == 3 else it == 4
    }, 7)
} returns 6

println(obj.manyMany(5, 6, 3, 4, 7)) // 6
println(obj.manyMany(5, 6, 3, 4, 4, 7)) // 6
```

### Private functions mocking / dynamic calls

IF you need to mock private functions, you can do it via a dynamic call.
```kotlin
class Car {
    fun drive() = accelerate()

    private fun accelerate() = "going faster"
}

val mock = spyk<Car>(recordPrivateCalls = true)

every { mock["accelerate"]() } returns "going not so fast"

assertEquals("going not so fast", mock.drive())

verifySequence {
    mock.drive()
    mock["accelerate"]()
}
```

If you want to verify private calls, you should create a `spyk` with `recordPrivateCalls = true`

Additionally, a more verbose syntax allows you to get and set properties, combined with the same dynamic calls:

```kotlin
val mock = spyk(Team(), recordPrivateCalls = true)

every { mock getProperty "speed" } returns 33
every { mock setProperty "acceleration" value less(5) } just runs
justRun { mock invokeNoArgs "privateMethod" }
every { mock invoke "openDoor" withArguments listOf("left", "rear") } returns "OK"

verify { mock getProperty "speed" }
verify { mock setProperty "acceleration" value less(5) }
verify { mock invoke "openDoor" withArguments listOf("left", "rear") }
```

### Property backing fields

You can access the backing fields via `fieldValue` and use `value` for the value being set.

Note: in the examples below, we use `propertyType` to specify the type of the `fieldValue`.
This is needed because it is possible to capture the type automatically for the getter.
Use `nullablePropertyType` to specify a nullable type.

**Note:** This is only for public fields. It is nearly impossible to mock private properties as they don't have getter methods attached. Use Java reflection to make the field accessible or use `@VisibleForTesting` annotation in the source.

```kotlin
val mock = spyk(MockCls(), recordPrivateCalls = true)

every { mock.property } answers { fieldValue + 6 }
every { mock.property = any() } propertyType Int::class answers { fieldValue += value }
every { mock getProperty "property" } propertyType Int::class answers { fieldValue + 6 }
every { mock setProperty "property" value any<Int>() } propertyType Int::class answers  { fieldValue += value }
every {
    mock.property = any()
} propertyType Int::class answers {
    fieldValue = value + 1
} andThen {
    fieldValue = value - 1
}
```

### Multiple interfaces

Adding additional behaviours via interfaces and stubbing them:

```kotlin
val spy = spyk(System.out, moreInterfaces = arrayOf(Runnable::class))

spy.println(555)

every {
    (spy as Runnable).run()
} answers {
    (self as PrintStream).println("Run! Run! Run!")
}

val thread = Thread(spy as Runnable)
thread.start()
thread.join()
```

### Mocking Nothing

Nothing special here. If you have a function returning `Nothing`:

```kotlin
fun quit(status: Int): Nothing {
    exitProcess(status)
}
```

Then you can for example throw an exception as behaviour:

```kotlin
every { quit(1) } throws Exception("this is a test")
```

### Clearing vs Unmocking

* clear - deletes the internal state of objects associated with a mock, resulting in an empty object
* unmock - re-assigns transformation of classes back to original state prior to mock

### Scoped mocks

A Scoped mock is a mock that automatically unmocks itself after the code block passed as a parameter has been executed.
You can use the `mockkObject`, `mockkStatic` and `mockkConstructor` functions.

```kotlin
object ObjBeingMocked {
 fun add(a: Int, b: Int) = a + b
}

// ObjBeingMocked will be unmocked after this scope
mockkObject(ObjBeingMocked) {
 assertEquals(3, ObjBeingMocked.add(1, 2))
 every { ObjBeingMocked.add(1, 2) } returns 55
 assertEquals(55, ObjBeingMocked.add(1, 2))
}
```

### Suppressing superclass calls

To suppress a method call, especially a `super` call inside an overridden method, you can stub its behavior on a `spyk`.
Using `every { ... } just runs` replaces the entire method body, preventing the original code from executing.

This is particularly useful for users coming from frameworks like PowerMockito or for testing classes like Android Activities.
For reference, see [PowerMockito suppress documentation](https://github.com/powermock/powermock/wiki/Suppress-Unwanted-Behavior).

```kotlin
// A simple inheritance hierarchy
open class Parent {
    var superCalled = false
    open fun doWork() {
        superCalled = true
    }
}

class Child : Parent() {
    override fun doWork() {
        super.doWork() // We want to suppress this call during testing
    }
}

// In your test:
val child = spyk<Child>()

// Stub the method: prevents the original (super) implementation from being invoked
every { child.doWork() } just runs

child.doWork()

// Verify that the superclass method was not executed
assertFalse(child.superCalled)
```

This approach allows you to isolate the logic within your method for unit testing without executing unwanted parent class behavior.

## Matcher extensibility

A very simple way to create new matchers is by attaching a function
to `MockKMatcherScope` or `MockKVerificationScope` and using the `match` function:

```kotlin
fun MockKMatcherScope.seqEq(seq: Sequence<String>) = match<Sequence<String>> {
    it.toList() == seq.toList()
}
```

It's also possible to create more advanced matchers by implementing the `Matcher` interface.

### Custom matchers

Example of a custom matcher that compares list without order:

```kotlin
@Test
fun test() {
    class MockCls {
        fun op(a: List<Int>) = a.reversed()
    }

    val mock = mockk<MockCls>()

    every { mock.op(any()) } returns listOf(5, 6, 9)

    println(mock.op(listOf(1, 2, 3)))

    verify { mock.op(matchListWithoutOrder(3, 2, 1)) }

}

data class ListWithoutOrderMatcher<T>(
    val expectedList: List<T>,
    val refEq: Boolean
) : Matcher<List<T>> {
    val map = buildCountsMap(expectedList, refEq)

    override fun match(arg: List<T>?): Boolean {
        if (arg == null) return false
        return buildCountsMap(arg, refEq) == map
    }

    private fun buildCountsMap(list: List<T>, ref: Boolean): Map<Any?, Int> {
        val map = mutableMapOf<Any?, Int>()

        for (item in list) {
            val key = when {
                item == null -> nullKey
                refEq -> InternalPlatform.ref(item)
                else -> item
            }
            map.compute(key, { _, value -> (value ?: 0) + 1 })
        }

        return map
    }

    override fun toString() = "matchListWithoutOrder($expectedList)"

    @Suppress("UNCHECKED_CAST")
    override fun substitute(map: Map<Any, Any>): Matcher<List<T>> {
        return copy(expectedList = expectedList.map { map.getOrDefault(it as Any?, it) } as List<T>)
    }

    companion object {
        val nullKey = Any()
    }
}

inline fun <reified T : List<E>, E : Any> MockKMatcherScope.matchListWithoutOrder(
    vararg items: E,
    refEq: Boolean = true
): T = match(ListWithoutOrderMatcher(listOf(*items), refEq))
```

### Reflection matchers

Example using reflection to mock all methods on a builder-style object

```kotlin
val builderFunctions = MyBuilder::class.memberFunctions.filter { it.returnType.classifier == MyBuilder::class }
val builderMock = mockk<MyBuilder> {
  builderFunctions.forEach { func ->
    every {
      val params = listOf<Any?>(builderMock) + func.parameters.drop(1).map { any(it.type.classifier as KClass<Any>) }
      func.call(*params.toTypedArray())
    } answers {
      this@mockk
    }
  }
}
```

## Configuration File

To adjust parameters globally, you can specify settings in a configuration file.

### How to use:

1. Create the configuration file at `src/test/resources/mockk.properties`.
2. Add any of the following configuration options:

```properties
# MockK Settings
relaxed=true|false
relaxUnitFun=true|false
recordPrivateCalls=true|false
stackTracesOnVerify=true|false
stackTracesAlignment=left|center
failOnSetBackingFieldException=true|false

# Restricted Mocking Configuration (see Restricted Mocking section below)
mockk.restrictedClasses=com.example.MyClass,com.example.AnotherClass
mockk.throwExceptionOnBadMock=true|false
```

### Configuration Options

| **Property**                    | **Description**                                                                                 | **Default Value** |
|---------------------------------|-------------------------------------------------------------------------------------------------|-------------------|
| `relaxed`                       | Enable relaxed mocking globally                                                                 | `false`           |
| `relaxUnitFun`                  | Enable relaxed mocking for Unit-returning functions                                             | `false`           |
| `recordPrivateCalls`            | Record private calls for verification                                                           | `false`           |
| `stackTracesOnVerify`           | Show stack traces on verification failures                                                      | `true`            |
| `stackTracesAlignment`          | Align stack traces to `left` or `center`                                                        | `center`          |
| `failOnSetBackingFieldException`| Fail tests if backing field cannot be set                                                       | `false`           |
| `mockk.restrictedClasses`       | Add fully qualified names of classes to restrict from mocking (comma-separated)                 | N/A               |
| `mockk.throwExceptionOnBadMock` | Throw exception when mocking restricted classes (`true`), or log warning only (`false`)         | `false`           |

**Notes:**
* `stackTracesAlignment` determines whether to align the stack traces to the center (default),
 or to the left (more consistent with usual JVM stackTraces).
* If `failOnSetBackingFieldException` is set to `true`, tests fail if a backing field could not be set.
 Otherwise, only the warning "Failed to set backing field" will be logged.
 See [here](https://github.com/mockk/mockk/issues/1291) for more details.

### Legacy Configuration

For backward compatibility, MockK also supports the legacy configuration file:
```
src/test/resources/io/mockk/settings.properties
```

If both files exist, `mockk.properties` takes precedence. The legacy location is deprecated and will be removed in a future version.

## DSL tables

Here are a few tables to help you master the DSL.

### Top level functions

| Function                  | Description                                                                                                |
|---------------------------|------------------------------------------------------------------------------------------------------------|
| `mockk<T>(...)`           | builds a regular mock                                                                                      |
| `spyk<T>()`               | builds a spy using the default constructor                                                                 |
| `spyk(obj)`               | builds a spy by copying from `obj`                                                                         |
| `slot`                    | creates a capturing slot                                                                                   |
| `every`                   | starts a stubbing block                                                                                    |
| `coEvery`                 | starts a stubbing block for coroutines                                                                     |
| `verify`                  | starts a verification block                                                                                |
| `coVerify`                | starts a verification block for coroutines                                                                 |
| `verifyAll`               | starts a verification block that should include all calls                                                  |
| `coVerifyAll`             | starts a verification block that should include all calls for coroutines                                   |
| `verifyOrder`             | starts a verification block that checks the order                                                          |
| `coVerifyOrder`           | starts a verification block that checks the order for coroutines                                           |
| `verifySequence`          | starts a verification block that checks whether all calls were made in a specified sequence                |
| `coVerifySequence`        | starts a verification block that checks whether all calls were made in a specified sequence for coroutines |
| `excludeRecords`          | exclude some calls from being recorded                                                                     |
| `confirmVerified`         | confirms that all recorded calls were verified                                                             |
| `checkUnnecessaryStub`    | confirms that all recorded calls are used at least once                                                    |
| `clearMocks`              | clears specified mocks                                                                                     |
| `registerInstanceFactory` | allows you to redefine the way of instantiation for certain object                                         |
| `mockkClass`              | builds a regular mock by passing the class as parameter                                                    |
| `mockkObject`             | turns an object into an object mock, or clears it if was already transformed                               |
| `unmockkObject`           | turns an object mock back into a regular object                                                            |
| `mockkStatic`             | makes a static mock out of a class, or clears it if it was already transformed                             |
| `unmockkStatic`           | turns a static mock back into a regular class                                                              |
| `clearStaticMockk`        | clears a static mock                                                                                       |
| `mockkConstructor`        | makes a constructor mock out of a class, or clears it if it was already transformed                        |
| `unmockkConstructor`      | turns a constructor mock back into a regular class                                                         |
| `clearConstructorMockk`   | clears the constructor mock                                                                                |
| `unmockkAll`              | unmocks object, static and constructor mocks                                                               |
| `clearAllMocks`           | clears regular, object, static and constructor mocks                                                       |


### Matchers

By default, simple arguments are matched using `eq()`

| Matcher                                                 | Description                                                                                            |
|---------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `any<T>()`                                              | matches any argument of type `T` (type-checked)                                                        |
| `any(Class)`                                            | matches any argument of the given Class (type-checked; useful for reflective mocking)                  |
| `allAny()`                                              | special matcher that uses `any()` instead of `eq()` for matchers that are provided as simple arguments |
| `isNull()`                                              | checks if the value is null                                                                            |
| `isNull(inverse=true)`                                  | checks if the value is not null                                                                        |
| `ofType(type)`                                          | checks if the value belongs to the type                                                                |
| `match { it.startsWith("string") }`                     | matches via the passed predicate                                                                       |
| `coMatch { it.startsWith("string") }`                   | matches via the passed coroutine predicate                                                             |
| `matchNullable { it?.startsWith("string") }`            | matches nullable value via the passed predicate                                                        |
| `coMatchNullable { it?.startsWith("string") }`          | matches nullable value via the passed coroutine predicate                                              |
| `eq(value)`                                             | matches if the value is equal to the provided value via the `deepEquals` function                      |
| `eq(value, inverse=true)`                               | matches if the value is not equal to the provided value via the `deepEquals` function                  |
| `neq(value)`                                            | matches if the value is not equal to the provided value via the `deepEquals` function                  |
| `refEq(value)`                                          | matches if the value is equal to the provided value via reference comparison                           |
| `refEq(value, inverse=true)`                            | matches if the value is not equal to the provided value via reference comparison                       |
| `nrefEq(value)`                                         | matches if the value is not equal to the provided value via reference comparison                       |
| `cmpEq(value)`                                          | matches if the value is equal to the provided value via the `compareTo` function                       |
| `less(value)`                                           | matches if the value is less than the provided value via the `compareTo` function                      |
| `more(value)`                                           | matches if the value is more than the provided value via the `compareTo` function                      |
| `less(value, andEquals=true)`                           | matches if the value is less than or equal to the provided value via the `compareTo` function          |
| `more(value, andEquals=true)`                           | matches if the value is more than or equal to the provided value via the `compareTo` function          |
| `range(from, to, fromInclusive=true, toInclusive=true)` | matches if the value is in range via the `compareTo` function                                          |
| `and(left, right)`                                      | combines two matchers via a logical and                                                                |
| `or(left, right)`                                       | combines two matchers via a logical or                                                                 |
| `not(matcher)`                                          | negates the matcher                                                                                    |
| `capture(slot)`                                         | captures a Non Nullable value to a `CapturingSlot`                                                     |
| `captureNullable(slot)`                                 | captures a Nullable value to a `CapturingSlot`                                                         |
| `capture(mutableList)`                                  | captures a value to a list                                                                             |
| `captureNullable(mutableList)`                          | captures a value to a list together with null values                                                   |
| `captureLambda()`                                       | captures a lambda                                                                                      |
| `captureCoroutine()`                                    | captures a coroutine                                                                                   |
| `invoke(...)`                                           | calls a matched argument                                                                               |
| `coInvoke(...)`                                         | calls a matched argument for a coroutine                                                               |
| `hint(cls)`                                             | hints the next return type in case it's gotten erased                                                  |
| `anyVararg()`                                           | matches any elements in a vararg                                                                       |
| `varargAny(matcher)`                                    | matches if any element matches the matcher                                                             |
| `varargAll(matcher)`                                    | matches if all elements match the matcher                                                              |
| `any...Vararg()`                                        | matches any elements in vararg (specific to primitive type)                                            |
| `varargAny...(matcher)`                                 | matches if any element matches the matcher (specific to the primitive type)                            |
| `varargAll...(matcher)`                                 | matches if all elements match the matcher (specific to the primitive type)                             |

A few special matchers available in verification mode only:

| Matcher                      | Description                                                          |
|------------------------------|----------------------------------------------------------------------|
| `withArg { code }`           | matches any value and allows to execute some code                    |
| `withNullableArg { code }`   | matches any nullable value and allows to execute some code           |
| `coWithArg { code }`         | matches any value and allows to execute some coroutine code          |
| `coWithNullableArg { code }` | matches any nullable value and allows to execute some coroutine code |

### Validators

| Validator                                       | Description                                                                                     |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `verify { mock.call() }`                        | Do unordered verification that a call was performed                                             |
| `verify(inverse=true) { mock.call() }`          | Do unordered verification that a call was not performed                                         |
| `verify(atLeast=n) { mock.call() }`             | Do unordered verification that a call was performed at least `n` times                          |
| `verify(atMost=n) { mock.call() }`              | Do unordered verification that a call was performed at most `n` times                           |
| `verify(exactly=n) { mock.call() }`             | Do unordered verification that a call was performed exactly `n` times                           |
| `verifyAll { mock.call1(); mock.call2() }`      | Do unordered verification that only the specified calls were executed for the mentioned mocks   |
| `verifyOrder { mock.call1(); mock.call2() }`    | Do verification that the sequence of calls went one after another                               |
| `verifySequence { mock.call1(); mock.call2() }` | Do verification that only the specified sequence of calls were executed for the mentioned mocks |
| `verify { mock wasNot Called }`                 | Do verification that a mock was not called                                                      |
| `verify { listOf(mock1, mock2) wasNot Called }` | Do verification that a list of mocks were not called                                            |

### Answers

An Answer can be followed up by one or more additional answers.

| Answer                       | Description                                                                                                        |
|------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `returns value`              | specify that the matched call returns a specified value                                                            |
| `returnsMany list`           | specify that the matched call returns a value from the list, with subsequent calls returning the next element      |
| `returnsArgument(n)`         | specify that the matched call returns the nth argument of that call                                                |
| `throws ex`                  | specify that the matched call throws an exception                                                                  |
| `throwsMany list`            | specify that the matched call throws an exception from the list, with subsequent calls throwing the next exception |
| `answers { code }`           | specify that the matched call answers with a code block scoped with `answer scope`                                 |
| `coAnswers { code }`         | specify that the matched call answers with a coroutine code block  with `answer scope`                             |
| `answers answerObj`          | specify that the matched call answers with an Answer object                                                        |
| `answers { nothing }`        | specify that the matched call answers null                                                                         |
| `just Runs`                  | specify that the matched call is returning Unit (returns null)                                                     |
| `just Awaits`                | specify that the matched call never returns (available since v1.13.3)                                              |
| `propertyType Class`         | specify the type of the backing field accessor                                                                     |
| `nullablePropertyType Class` | specify the type of the backing field accessor as a nullable type                                                  |


### Additional answer(s)

A next answer is returned on each consequent call and the last value is persisted.
So this is similar to the `returnsMany` semantics.

| Additional answer         | Description                                                                                                        |
|---------------------------|--------------------------------------------------------------------------------------------------------------------|
| `andThen value`           | specify that the matched call returns one specified value                                                          |
| `andThenMany list`        | specify that the matched call returns a value from the list, with subsequent calls returning the next element      |
| `andThenThrows ex`        | specify that the matched call throws an exception                                                                  |
| `andThenThrowsMany ex`    | specify that the matched call throws an exception from the list, with subsequent calls throwing the next exception |
| `andThen { code }`        | specify that the matched call answers with a code block scoped with `answer scope`                                 |
| `coAndThen { code }`      | specify that the matched call answers with a coroutine code block with `answer scope`                              |
| `andThenAnswer answerObj` | specify that the matched call answers with an Answer object                                                        |
| `andThen { nothing }`     | specify that the matched call answers null                                                                         |
| `andThenJust Runs`        | specify that the matched call is returning Unit (available since v1.12.2)                                          |
| `andThenJust Awaits`      | specify that the matched call is never returning (available since v1.13.3)                                         |

### Answer scope

| Parameter                     | Description                                                             |
|-------------------------------|-------------------------------------------------------------------------|
| `call`                        | a call object that consists of an invocation and a matcher              |
| `invocation`                  | contains information regarding the actual function invoked              |
| `matcher`                     | contains information regarding the matcher used to match the invocation |
| `self`                        | reference to the object invocation made                                 |
| `method`                      | reference to the function invocation made                               |
| `args`                        | reference to the invocation arguments                                   |
| `nArgs`                       | number of invocation arguments                                          |
| `arg(n)`                      | nth argument                                                            |
| `firstArg()`                  | first argument                                                          |
| `secondArg()`                 | second argument                                                         |
| `thirdArg()`                  | third argument                                                          |
| `lastArg()`                   | last argument                                                           |
| `captured()`                  | the last element in the list for convenience when capturing to a list   |
| `lambda<...>().invoke()`      | call the captured lambda                                                |
| `coroutine<...>().coInvoke()` | call the captured coroutine                                             |
| `nothing`                     | null value for returning `nothing` as an answer                         |
| `fieldValue`                  | accessor to the property backing field                                  |
| `fieldValueAny`               | accessor to the property backing field with `Any?` type                 |
| `value`                       | value being set, cast to the same type as the property backing field    |
| `valueAny`                    | value being set, with `Any?` type                                       |
| `callOriginal`                | calls the original function                                             |

### Vararg scope

| Parameter  | Description                                   |
|------------|-----------------------------------------------|
| `position` | the position of an argument in a vararg array |
| `nArgs`    | overall count of arguments in a vararg array  |

## Restricted Mocking in MockK

### Overview

**Restricted Mocking** is a feature in MockK designed to **prevent the mocking of classes** that are problematic to mock.
These classes often indicate poor test design and can lead to **unreliable** or **misleading test results**.

The primary goal is to:
- **Encourage better testing practices**
- **Promote code maintainability**
- **Avoid mocking classes tied to system operations or critical data structures**


### Why Restrict Mocking?

Mocking certain classes can cause several issues:

| 🚩 **Problem**              | ⚠️ **Impact**                                                               |
|-----------------------------|-----------------------------------------------------------------------------|
| **False sense of security** | Tests may pass even when the implementation is fragile or incorrect.        |
| **Tight coupling**          | Tests become overly dependent on low-level implementation details.          |
| **Hard-to-maintain tests**  | Changes in code can break unrelated tests, increasing maintenance overhead. |
| **Code smells**             | Mocking system-level or value-type classes often signals poor architecture. |


### Default Restricted Classes

The following classes are **restricted from being mocked by default**:

| **Class**              | **Description**                                                       | **Includes Subtypes?** |
|------------------------|-----------------------------------------------------------------------|------------------------|
| `java.lang.System`     | System-related APIs (`System.currentTimeMillis()`, `System.getenv()`) | ✅ Yes                  |
| `java.util.Collection` | Collections like `List`, `Set`, and `Queue`                           | ✅ Yes                  |
| `java.util.Map`        | Key-value data structures like `HashMap`                              | ✅ Yes                  |
| `java.io.File`         | File I/O classes (should be abstracted instead)                       | ✅ Yes                  |
| `java.nio.file.Path`   | Path manipulation classes for file systems                            | ✅ Yes                  |

⚠️ **Note:**
**All subclasses and implementations** of these classes are also restricted.
For example:
- `ArrayList` and `HashSet` (subtypes of `Collection`)
- `HashMap` (subtype of `Map`)
- Custom classes that extend `File` or implement `Path`

### How to Configure Restricted Mocking

You can configure Restricted Mocking behavior using the `mockk.properties` configuration file described in the [Configuration File](#configuration-file) section.

Add the following properties to your [configuration file](#configuration-file):

```properties
# List of restricted classes (fully qualified names, separated by commas)
mockk.restrictedClasses=com.foo.Bar,com.foo.Baz

# Whether to throw an exception when mocking restricted classes
mockk.throwExceptionOnBadMock=true
```

See the [Configuration File](#configuration-file) section for detailed information about these and other available options.

⚠️ **Note:**

If `mockk.throwExceptionOnBadMock` is not set, it will default to `false`, meaning only warnings will be logged.

To strictly prevent mocking restricted classes, explicitly set:
```properties
mockk.throwExceptionOnBadMock=true
```

### Configure Restricted Mocking with System Properties

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

### Configure Restricted Mocking with Gradle Properties

You can add the property to your `gradle.properties` file:
```properties
systemProp.mockk.throwExceptionOnBadMock=true
```
Then in your terminal:

```
./gradlew -Pmockk.throwExceptionOnBadMock=true :modules:name:test
```

⚠️ **Note:**

The `mockk.throwExceptionOnBadMock` that is set in `gradle.properties` or
in your `build.gradle` will override the value that is set in `mockk.properties` file.

### Behavior When Mocking Restricted Classes

#### When `mockk.throwExceptionOnBadMock=false` (Default)

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

#### When `mockk.throwExceptionOnBadMock=true`
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

### Custom Class Restriction Example

You can restrict **custom classes** from being mocked using the `mockk.properties` configuration file.
This helps enforce proper testing practices even within your own codebase.

#### Example 1: Mocking a Restricted Custom Class (Throws Exception)

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

## Funding

You can also support this project by becoming a sponsor. Your logo will show up here with a link to your website.

### Sponsors

<a href="https://opencollective.com/mockk/sponsor/0/website" target="_blank">
  <img src="https://opencollective.com/mockk/sponsor/0/avatar.svg"/>
</a>
<a href="https://opencollective.com/mockk/sponsor/1/website" target="_blank">
  <img src="https://opencollective.com/mockk/sponsor/1/avatar.svg"/>
</a>

### Backers

Thank you to all our backers! 🙏

<a href="https://opencollective.com/mockk#backers" target="_blank">
  <img src="https://opencollective.com/mockk/backers.svg?width=890"/>
</a>

### Contributors

This project exists thanks to all the people who contribute.

<a href="https://github.com/mockk/mockk/graphs/contributors">
  <img src="https://opencollective.com/mockk/contributors.svg?width=890" />
</a>

## Getting Help

To ask questions, please use Stack Overflow or Gitter.

* Chat/Gitter: [https://gitter.im/mockk-io/Lobby](https://gitter.im/mockk-io/Lobby)
* Stack Overflow: [http://stackoverflow.com/questions/tagged/mockk](https://stackoverflow.com/questions/tagged/mockk)

To report bugs, please use the GitHub project.

* Project Page: [https://github.com/mockk/mockk](https://github.com/mockk/mockk)
* Reporting Bugs: [https://github.com/mockk/mockk/issues](https://github.com/mockk/mockk/issues)
